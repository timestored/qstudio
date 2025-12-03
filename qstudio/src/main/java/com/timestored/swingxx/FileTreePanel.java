/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.swingxx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.common.base.Preconditions;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.DirWatch;
import com.timestored.misc.DirWatch.DirWatchListener;
import com.timestored.misc.FifoBuffer;
import com.timestored.misc.IOUtils;
import com.timestored.sqldash.chart.JdbcChartPanel;
import com.timestored.swingxx.JTreeHelper.IdentifiableNode;
import com.timestored.theme.Theme;



/**
 * Displays selected folder as a {@link JTree} using system icons.
 */
public class FileTreePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(FileTreePanel.class.getName());
	private static final String DEFAULT_IGNORE_FOLDER_REGEX = "^\\..*|^target$";
	public static final FileFilter IGNORE_SVN_FILTER =  DirWatch.generateFileFilter(Pattern.compile(DEFAULT_IGNORE_FOLDER_REGEX));
	public static boolean TEST_MODE = true;  // Tests need forced instant render. End user UI not really.

	
	private static FileSystemView fsv = FileSystemView.getFileSystemView();
	private JTree tree;
	private File root;
	/** component shown when no roots set */
	private Component noRootsComponent;
	private final CopyOnWriteArrayList<Listener> listeners;
	private final TreeMouseListener treeMouseListener;
	private boolean rightClickMenuShown = true;
	private FileFilter fileFilter;

	private final DirWatch dirWatch;
	private final FifoBuffer<File> fileCache = new FifoBuffer<File>(DirWatch.MAX_FILES_TO_WATCH);
	private final FileTreeCellRenderer fileTreeCellRenderer;
	private Pattern ignoredFoldersRegex;

	/** 
	 * Set the topmost level files of this tree, {@link File#listFiles()} is handy. 
	 */
	public FileTreePanel() {
		
		this.listeners = new CopyOnWriteArrayList<Listener>();
		this.fileFilter = IGNORE_SVN_FILTER;
		this.dirWatch = new DirWatch(30100, fileFilter, true); // 1 minute for now
		this.setLayout(new BorderLayout());
		noRootsComponent = new JLabel(" No root folder selected");
		noRootsComponent.setName("noRootsComponent");
		
		treeMouseListener = new TreeMouseListener();
		addMouseListener(new RefreshTreeMouseListener());
		refreshGui();
		dirWatch.addListener(new DirWatchListener() {
			@Override public void changeOccurred() {
				refreshGui();
			}
		});
		

		fileTreeCellRenderer = new FileTreeCellRenderer();
		fileTreeCellRenderer.addListener(new FileTreeCellRenderer.Listener() {
			@Override public void renderedFile(File file) {
				fileCache.add(file);
			}
		}); 
	}
	
	/**
	 * @return A Cache of most files displayed within the tree that is kept up 
	 * to date within a time window by monitoring the file system.
	 * If there are greater than dirWatch.MAX_FILES_TO_WATCH only those
	 * shown in the tree will be in the cache.
	 */
	public Collection<File> getFileCache() {
		return fileCache.getAll();
	}

	private JPopupMenu getFileRightClickMenu(final File file) {
		
		JPopupMenu popupMenu = new JPopupMenu("");
		popupMenu.setName("fileRightClickMenu");
		
		final File contextFile = (file==null ? root : file);
		File f = contextFile;
		if(!f.isDirectory()) {
			f = contextFile.getParentFile();
		}
		final File nearestDir = f;
		
		// open explorer window here
		final String openText = contextFile.isDirectory() ? "Open Folder" : "Open Containing Folder";
		popupMenu.add(new AAction(openText, e -> {
				if(nearestDir.isDirectory()) {
					try {
						Desktop.getDesktop().open(nearestDir);
					} catch (IOException ioe) {
						String msg = "Could not open folder";
						LOG.log(Level.WARNING, msg, ioe);
						JOptionPane.showMessageDialog(null, msg);
					}
				}
			}));
		
		
		popupMenu.add(new AAction(Msg.get(Key.CREATE_NEW_FOLDER), Theme.CIcon.FOLDER_ADD.get16(), e -> {
			String newFolderName = JOptionPane.showInputDialog("Enter name for new Folder:", "New Folder");
			if(newFolderName != null) {
				File ff = new File(nearestDir, newFolderName);
				ff.mkdirs();
				ff.mkdir();
				refreshGui();
			}}));
		
		
		File pages = new File(root,"pages");
		boolean isParent = false; 
	    try {
	    	isParent = file.getCanonicalPath().startsWith(pages.getCanonicalPath());
		} catch (IOException e1) {}
	    final boolean wasUnderPages = isParent;
	    
		
		popupMenu.add(new AbstractAction(Msg.get(Key.CREATE_NEW_FILE), Theme.CIcon.PAGE.get16()) {
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				String newFileName = JOptionPane.showInputDialog("Enter name for new file:", "New File");
				String errMsg = "Could not create new File";
				if(newFileName != null) {
					String n = (wasUnderPages && !newFileName.contains(".")) ? (newFileName + ".md") : newFileName;
					File f = new File(nearestDir, n);
					boolean success = true;
					f.getParentFile().mkdirs();
					try {
						success = f.createNewFile();
					} catch (IOException ioe) {
						success = false;
						LOG.log(Level.WARNING, errMsg, ioe);
					}
					if(!success) {
						JOptionPane.showMessageDialog(null, errMsg);
					}
					refreshGui();
				}
			}
		});
		

		popupMenu.add(new AAction("Refresh Tree",e -> refreshGui()));
		
		return popupMenu;
	}

	/**
	 * jtree with more sane, right click to popup menu, left click to actually
	 * select file.
	 */
	private class TreeMouseListener extends MouseAdapter {

		@Override public void mouseClicked(MouseEvent e) {

			if (SwingUtilities.isRightMouseButton(e)) {
				int row = tree.getClosestRowForLocation(e.getX(), e.getY());
				tree.setSelectionRow(row);
			}
			
			// check actual file was selected
			File f = null;
			TreePath tp = tree.getSelectionPath();
			Object o = tp==null ? null : tp.getLastPathComponent();
			if (o != null && o instanceof FileTreeNode) {
				f = ((FileTreeNode) o).file;
			}

			// confirm it still exists else refresh tree.
			if(f!= null && !f.canRead()) {
				refreshGui();
			} else if (f != null) {
				// show menu or report as selected
				if (SwingUtilities.isRightMouseButton(e) && rightClickMenuShown) {
					getFileRightClickMenu(f).show(e.getComponent(), e.getX(), e.getY());
				} else if (SwingUtilities.isLeftMouseButton(e)) {
					LOG.info("FileTreePanel file selected->" + f);
					for (Listener l : listeners) {
						l.fileSelected(f);
					}
				}
			}
		}
	}
	

	private class RefreshTreeMouseListener extends MouseAdapter {

		@Override public void mouseClicked(MouseEvent e) {
			if (root != null) {
				// show menu or report as selected
				if (SwingUtilities.isRightMouseButton(e) && rightClickMenuShown) {
					getFileRightClickMenu(root).show(e.getComponent(), e.getX(), e.getY());
				} 
			}
		}
	}

	
	private static String[] ENDS = new String[] { "exe","dll","zip","rar","7z","iso","msi","dmg","docx","xlsx","pptx","pdf","epub","mobi","mp3","wav","wma","flac","aac","mp4","avi","mkv","mov","wmv","gif","jpg","jpeg","png","bmp","tiff","psd"};
	
	private static boolean isKnownBinaryFormat(File f) {
		String e = IOUtils.getFileEnding(f).toLowerCase();
		return Arrays.stream(ENDS).anyMatch(s -> s.equals(e));
	}

	public static void openFileOrBrowseTo(File f, Consumer<File> fileOpener) {
		if(f != null) {
			if(f.isFile() && !FileTreePanel.isKnownBinaryFormat(f)) {
				fileOpener.accept(f);
			} else {
				try {
					Desktop.getDesktop().open(f);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(null, "Could not open folder");
				}
			}
		}
	}
	
	/**
	 * Interface to receive events from this file choosing tree.
	 */@FunctionalInterface
	public static interface Listener {
		public abstract void fileSelected(File selectedFile);
	}
	
	/**
	 * Refresh the entire appearance of the folders/files.
	 */
	public void refreshGui() {
		//TODO separate freshing of folder struct and GUI display
		// as network drive walking is VERY slow and could freeze GUI
		
		// check root still exists and is valid directory
		File r = root;
		if(r != null) {
			if(!r.isDirectory() || !r.canRead()) {
				r = null;
			}
		}
		File[] files = r != null ? DirWatch.getFiles(fileFilter, root) : null;
		JdbcChartPanel.runInUIthread(() -> refreshUI(files), TEST_MODE);
	}

	private void refreshUI(File[] files) {
		LOG.info("FileTreePanel refreshGui");
		if(tree != null) {
			tree.removeMouseListener(treeMouseListener);
		}
		if(files == null) {
			removeAll();
			add(noRootsComponent, BorderLayout.CENTER);
		} else {
			files = DirWatch.getFiles(fileFilter, root);
			fileCache.addAll(DirWatch.generateFileCache(files, fileFilter));
			
			if (files.length > 0){
				FileTreeNode rootTreeNode = new FileTreeNode(files, fileFilter);
				
				tree = new JTree(rootTreeNode);
				tree.setName("FileTreePanelTree");
				tree.setCellRenderer(fileTreeCellRenderer);
				tree.setRootVisible(false);
				// Otherwise expanding can be very costly.
				boolean isLarge = IOUtils.containsMoreThanMaxFiles(root, 500);
				if(tree.getRowCount() < 20 && !isLarge) {
					for (int i = 0; i < tree.getRowCount(); i++) {
					    tree.expandRow(i);
					}
				}
				tree.addMouseListener(treeMouseListener);
				final JScrollPane jsp = new JScrollPane(this.tree);
				jsp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

				removeAll();
				add(jsp, BorderLayout.CENTER);
			} else {
				// empty folder
				removeAll();
				noRootsComponent = new JLabel(" Selected Folder is Empty");
				add(noRootsComponent, BorderLayout.CENTER);
			}
			revalidate();
			repaint();
		}
	}

	
	/**
	 * In the event roots is currently null, set the component that will be shown instead.
	 */
	public void setNoRootsComponent(Component noRootsComponent) {
		
		if(noRootsComponent == null) {
			throw new IllegalArgumentException("noRootsComponent cannot be null");
		}
		this.noRootsComponent = noRootsComponent;
		noRootsComponent.setName("noRootsComponent");
		if(root == null) {
			refreshGui();
		}
	}

	/**
	 * @param selectedFolder The directory that will be the root of the {@link JTree} shown.
	 */
	public void setRoot(File selectedFolder) {

		if(selectedFolder!=null && !selectedFolder.isDirectory()) {
			throw new IllegalArgumentException("setRoots file must be a directory");
		}

		boolean rootChanging = (selectedFolder==null && this.root!=null)
				|| (selectedFolder!=null && !selectedFolder.equals(this.root));
		
		if(rootChanging) {
			LOG.info("root changing");
			this.root = selectedFolder;
			if(selectedFolder == null) {
				dirWatch.stop();
			} else {
				try {
					if(IOUtils.containsMoreThanMaxFiles(selectedFolder, DirWatch.MAX_FILES_TO_WATCH)) {
						dirWatch.stop();
					} else {
						dirWatch.setRoot(selectedFolder);
					}
				} catch (Exception e) {
					LOG.warning("Could not watch root folder");
				}	
			}
			
			refreshGui();
		}
	}

	/** caching renderer for the file tree that uses system file icons. */
	private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		/** Icon cache to speed the rendering. */
		private Map<String, Icon> iconCache = new HashMap<String, Icon>();

		/**  Root name cache to speed the rendering. */
		private Map<File, String> rootNameCache = new HashMap<File, String>();
		
		private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
		
		private static interface Listener {
			void renderedFile(File file);
		}

		public void addListener(Listener listener) { listeners.add(listener); }
		public void clearListeners() { listeners.clear(); }
		
		@Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {
			if(value instanceof FileTreeNode) {
				FileTreeNode ftn = (FileTreeNode) value;
				File file = ftn.file;
				String filename = "";
				if (file != null) {
					for(Listener l : listeners) {
						l.renderedFile(file);
					}
					if (ftn.isFileSystemRoot) {
						// long start = System.currentTimeMillis();
						filename = this.rootNameCache.get(file);
						if (filename == null) {
							filename = fsv.getSystemDisplayName(file);
							this.rootNameCache.put(file, filename);
						}
						// long end = System.currentTimeMillis();
						// System.out.println(filename + ":" + (end - start));
					} else {
						filename = file.getName();
					}
				}
				JLabel result = (JLabel) super.getTreeCellRendererComponent(tree, filename, 
						sel, expanded, leaf, row, hasFocus);
				if (file != null) {
					Icon icon = this.iconCache.get(filename);
					if (icon == null) {
						// System.out.println("Getting icon of " + filename);
						icon = fsv.getSystemIcon(file);
						this.iconCache.put(filename, icon);
					}
					result.setIcon(icon);
				}
				return result;
			}
			return super.getTreeCellRendererComponent(tree, value, sel, expanded, 
					leaf, row, hasFocus);
		}
	}
	
	/** A node in the file tree.  */
	private static class FileTreeNode implements TreeNode,IdentifiableNode {

		private final File file;
		private File[] children;
		private final TreeNode parent;
		/** Indication whether this node corresponds to a file system root.	 */
		private boolean isFileSystemRoot;
		private final FileFilter fileFilter;

		/**
		 * Creates a new file tree node.
		 * @param file Node file
		 * @param isFileSystemRoot Indicates whether the file is a file system root.
		 * @param parent Parent node.
		 */
		public FileTreeNode(File file, boolean isFileSystemRoot, TreeNode parent, FileFilter fileFilter) {
			
			this.file = file;
			this.isFileSystemRoot = isFileSystemRoot;
			this.fileFilter = fileFilter;
			this.parent = parent;
			this.children = DirWatch.getFiles(fileFilter, file);
			
			if (this.children == null) {
				this.children = new File[0];
			}
			Arrays.sort(children, new FileFolderNameComparator());
		}
		
		private static class FileFolderNameComparator implements Comparator<File> {
			@Override public int compare(File f1, File f2) {
				if(f1.isDirectory() && f2.isFile()) {
					return -1;
				} else if(f2.isDirectory() && f1.isFile()) {
					return 1;
				}
				return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
			}
		}
		
		/**
		 * Creates a new root file tree node.
		 * @param children Children files.
		 */
		public FileTreeNode(File[] children, FileFilter fileFilter) {
			this.file = null;
			this.parent = null;
			Arrays.sort(children, new FileFolderNameComparator());
			this.children = children;
			this.fileFilter = fileFilter;
		}

		@Override public Enumeration<? extends TreeNode> children() {
			final int elementCount = this.children.length;
			return new Enumeration<TreeNode>() {

				int count = 0;

				@Override
				public boolean hasMoreElements() {
					return this.count < elementCount;
				}

				@Override
				public TreeNode nextElement() {
					if (this.count < elementCount) {
						return getChildAt(this.count++);
					}
					throw new NoSuchElementException("Vector Enumeration");
				}
			};

		}

		@Override public boolean getAllowsChildren() { return true; }

		@Override public TreeNode getChildAt(int childIndex) {
			return new FileTreeNode(this.children[childIndex], this.parent == null, this, fileFilter);
		}

		@Override public int getChildCount() { return this.children.length; }


		@Override public int getIndex(TreeNode node) {
			FileTreeNode ftn = (FileTreeNode) node;
			for (int i = 0; i < this.children.length; i++) {
				if (ftn.file.equals(this.children[i]))
					return i;
			}
			return -1;
		}

		@Override public TreeNode getParent() { return this.parent; }

		@Override public boolean isLeaf() { return (this.getChildCount() == 0); }

		@Override public String getId() { return file.getAbsolutePath(); }
	}

	/** Subscribe to file selection events */
	public void addListener(Listener listener) { listeners.add(listener); }
	
	public boolean isRightClickMenuShown() { return rightClickMenuShown; }

	public void setRightClickMenuShown(boolean rightClickMenuShown) {
		this.rightClickMenuShown = rightClickMenuShown;
	}
	
	public void setIgnoredFoldersRegex(final Pattern ignoredFoldersRegex) {
		Preconditions.checkNotNull(ignoredFoldersRegex);
		// if pattern changed, better re-parse folder.
		if(this.ignoredFoldersRegex == null || !ignoredFoldersRegex.pattern().equals(this.ignoredFoldersRegex.pattern())) {
			this.ignoredFoldersRegex = ignoredFoldersRegex;
			this.fileFilter = DirWatch.generateFileFilter(ignoredFoldersRegex);
			refreshGui();
		}
	}
}