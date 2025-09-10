# QStudio Web UI Components

This directory contains React/TypeScript components for the QStudio web interface.

## QueryableEditor Component

The `QueryableEditor` is a React component that provides an expandable code editor with the following features:

### Key Features

1. **Conditional Expansion**: The editor only expands when both `codeFocussed` AND `expandable` are true
2. **Persistent Settings**: Uses localStorage to remember the `expandable` setting
3. **Right-side Positioning**: When not expandable, keeps the editor positioned on the right-hand side
4. **Focus Management**: Tracks and responds to focus changes
5. **Tooltip Support**: Optional tooltip display

### Props

```typescript
interface QueryableEditorProps {
  showTooltip?: boolean;        // Show/hide tooltip
  codeFocussed?: boolean;       // External focus state
  children?: React.ReactNode;   // Editor content
  className?: string;           // Additional CSS classes
  onFocusChange?: (focused: boolean) => void; // Focus change callback
  style?: React.CSSProperties;  // Additional styles
}
```

### Usage

```tsx
import { QueryableEditor } from './components/QueryableEditor';

function App() {
  const [focused, setFocused] = useState(false);
  
  return (
    <QueryableEditor
      showTooltip={true}
      codeFocussed={focused}
      onFocusChange={setFocused}
    >
      SELECT * FROM users;
    </QueryableEditor>
  );
}
```

### Expansion Logic

The editor expands (fullscreen mode) when:
- `codeFocussed` is `true` AND
- `expandable` is `true` (stored in localStorage)

When not expanded:
- Editor maintains its position on the right-hand side
- Does not expand up and out of its container
- Maintains relative positioning

### Demo

Open `demo.html` in a browser to see the component in action.

### Testing

Run tests with:
```bash
npm test
```

### Building

Build the TypeScript code with:
```bash
npm run build
```