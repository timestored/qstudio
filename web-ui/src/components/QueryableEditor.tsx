import React, { useState, useEffect } from 'react';
import { useLocalStorage } from '../hooks/useLocalStorage';

export interface QueryableEditorProps {
  showTooltip?: boolean;
  codeFocussed?: boolean;
  children?: React.ReactNode;
  className?: string;
  onFocusChange?: (focused: boolean) => void;
  style?: React.CSSProperties;
}

export const QueryableEditor = (props: QueryableEditorProps) => {
  const [showTooltip, setShowTooltip] = useState(props.showTooltip);
  const [expandable, setExpandable] = useLocalStorage("expandable", true);
  const [codeFocussed, setCodeFocussed] = useState(props.codeFocussed || false);

  // Update showTooltip when props change
  useEffect(() => {
    setShowTooltip(props.showTooltip);
  }, [props.showTooltip]);

  // Update codeFocussed when props change
  useEffect(() => {
    setCodeFocussed(props.codeFocussed || false);
  }, [props.codeFocussed]);

  // Determine if the editor should be expanded
  // Only expand when both codeFocussed AND expandable are true
  const shouldExpand = codeFocussed && expandable;

  const handleFocus = () => {
    setCodeFocussed(true);
    props.onFocusChange?.(true);
  };

  const handleBlur = () => {
    setCodeFocussed(false);
    props.onFocusChange?.(false);
  };

  // Base styles for the editor container
  const baseStyles: React.CSSProperties = {
    position: 'relative',
    transition: 'all 0.3s ease-in-out',
    ...props.style,
  };

  // Apply expansion styles only when shouldExpand is true
  const expandedStyles: React.CSSProperties = shouldExpand
    ? {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1000,
        backgroundColor: 'white',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
      }
    : {
        // When not expandable, keep on right-hand side
        position: 'relative',
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'flex-start',
      };

  const containerStyles: React.CSSProperties = {
    ...baseStyles,
    ...expandedStyles,
  };

  const editorContentStyles: React.CSSProperties = shouldExpand
    ? {
        width: '100%',
        height: '100%',
        padding: '20px',
      }
    : {
        // Keep editor on right side when not expanded
        maxWidth: '50%',
        minWidth: '300px',
      };

  return (
    <div
      className={`queryable-editor ${shouldExpand ? 'expanded' : 'collapsed'} ${
        props.className || ''
      }`}
      style={containerStyles}
      onFocus={handleFocus}
      onBlur={handleBlur}
    >
      {/* Expandable toggle button */}
      <div
        style={{
          position: 'absolute',
          top: '10px',
          right: '10px',
          zIndex: 1001,
          display: 'flex',
          gap: '8px',
        }}
      >
        <button
          onClick={() => setExpandable(!expandable)}
          style={{
            padding: '4px 8px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            background: expandable ? '#007bff' : '#f8f9fa',
            color: expandable ? 'white' : '#333',
            cursor: 'pointer',
            fontSize: '12px',
          }}
          title={expandable ? 'Disable expansion' : 'Enable expansion'}
        >
          {expandable ? 'Expandable' : 'Fixed'}
        </button>
      </div>

      {/* Main editor content */}
      <div style={editorContentStyles}>
        {/* Tooltip display */}
        {showTooltip && (
          <div
            style={{
              position: 'absolute',
              top: '40px',
              left: '10px',
              background: '#333',
              color: 'white',
              padding: '8px 12px',
              borderRadius: '4px',
              fontSize: '12px',
              zIndex: 1002,
              whiteSpace: 'nowrap',
            }}
          >
            {shouldExpand ? 'Editor expanded (focus + expandable)' : 'Editor collapsed'}
          </div>
        )}

        {/* Editor content area */}
        <div
          role="textbox"
          aria-multiline="true"
          aria-label="Query editor"
          style={{
            width: '100%',
            height: shouldExpand ? 'calc(100% - 60px)' : '300px',
            border: `2px solid ${codeFocussed ? '#007bff' : '#ddd'}`,
            borderRadius: '4px',
            padding: '16px',
            backgroundColor: '#fafafa',
            fontFamily: 'monospace',
            fontSize: '14px',
            outline: 'none',
            overflow: 'auto',
          }}
          contentEditable
          suppressContentEditableWarning
          onFocus={handleFocus}
          onBlur={handleBlur}
        >
          {props.children || 'Type your query here...'}
        </div>

        {/* Status indicator */}
        <div
          style={{
            marginTop: '8px',
            fontSize: '12px',
            color: '#666',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <span>
            Status: {codeFocussed ? 'Focused' : 'Unfocused'} |{' '}
            {expandable ? 'Expandable' : 'Fixed Position'} |{' '}
            {shouldExpand ? 'EXPANDED' : 'Collapsed'}
          </span>
        </div>
      </div>
    </div>
  );
};