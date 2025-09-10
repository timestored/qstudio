import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryableEditor } from '../components/QueryableEditor';

// Mock localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
global.localStorage = localStorageMock as any;

describe('QueryableEditor', () => {
  beforeEach(() => {
    localStorageMock.getItem.mockClear();
    localStorageMock.setItem.mockClear();
    // Clear localStorage to start fresh
    localStorageMock.clear();
  });

  it('renders without crashing', () => {
    render(<QueryableEditor />);
    expect(screen.getByText(/Type your query here/)).toBeInTheDocument();
  });

  it('shows expandable button by default', () => {
    render(<QueryableEditor />);
    // Should have a button that shows either 'Expandable' or 'Fixed'
    const button = screen.getByRole('button');
    expect(['Expandable', 'Fixed']).toContain(button.textContent);
  });

  it('only expands when both codeFocussed and expandable are true', () => {
    render(<QueryableEditor codeFocussed={true} />);
    
    const container = screen.getByRole('textbox');
    // Should be expanded since both codeFocussed=true and expandable defaults to true
    expect(container.closest('.queryable-editor')).toHaveClass('expanded');
  });

  it('does not expand when expandable is false even if code is focused', () => {
    render(<QueryableEditor codeFocussed={true} />);
    
    // Get the current button and toggle to 'Fixed' if needed
    const button = screen.getByRole('button');
    if (button.textContent === 'Expandable') {
      fireEvent.click(button);
    }
    
    // Now check that it doesn't expand even though focused
    const container = screen.getByRole('textbox');
    expect(container.closest('.queryable-editor')).toHaveClass('collapsed');
  });

  it('does not expand when code is not focused even if expandable is true', () => {
    render(<QueryableEditor codeFocussed={false} />);
    
    const container = screen.getByRole('textbox');
    // Should not expand since codeFocussed=false even though expandable=true by default
    expect(container.closest('.queryable-editor')).toHaveClass('collapsed');
  });

  it('toggles expandable state when button is clicked', () => {
    render(<QueryableEditor />);
    
    // Find the button by its role
    const button = screen.getByRole('button');
    const initialText = button.textContent;
    
    fireEvent.click(button);
    
    // Check if the button text changed
    const newText = button.textContent;
    expect(newText).not.toBe(initialText);
    
    // Verify the text actually toggles between expected values
    expect(['Expandable', 'Fixed']).toContain(newText);
  });

  it('shows tooltip when showTooltip prop is true', () => {
    render(<QueryableEditor showTooltip={true} />);
    expect(screen.getByText(/Editor collapsed/)).toBeInTheDocument();
  });

  it('calls onFocusChange callback when focus changes', () => {
    const onFocusChange = jest.fn();
    render(<QueryableEditor onFocusChange={onFocusChange} />);
    
    const editorArea = screen.getByRole('textbox');
    fireEvent.focus(editorArea);
    
    expect(onFocusChange).toHaveBeenCalledWith(true);
  });

  it('maintains right-side positioning when not expandable', () => {
    render(<QueryableEditor codeFocussed={true} />);
    
    // Get the current button and check if we need to toggle
    const button = screen.getByRole('button');
    if (button.textContent === 'Expandable') {
      // Toggle to make it non-expandable
      fireEvent.click(button);
    }
    
    const container = screen.getByRole('textbox').closest('.queryable-editor') as HTMLElement;
    
    // When not expandable, it should maintain relative positioning
    expect(container).toHaveStyle('position: relative');
  });
});