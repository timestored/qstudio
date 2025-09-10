import React, { useState } from 'react';
import styled from 'styled-components';

const DialogOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const DialogContainer = styled.div`
  background: white;
  border-radius: 8px;
  padding: 24px;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
`;

const DialogTitle = styled.h2`
  margin: 0 0 20px 0;
  color: #333;
`;

const FormGroup = styled.div`
  margin-bottom: 16px;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
`;

const Input = styled.input`
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  
  &:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
  }
`;

const CheckboxContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const Checkbox = styled.input.attrs({ type: 'checkbox' })`
  margin: 0;
`;

const ButtonContainer = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
`;

const Button = styled.button.withConfig({
  shouldForwardProp: (prop) => prop !== 'variant',
})<{ variant?: 'primary' | 'secondary' }>`
  padding: 8px 16px;
  border: 1px solid ${props => props.variant === 'primary' ? '#007bff' : '#ddd'};
  background-color: ${props => props.variant === 'primary' ? '#007bff' : 'white'};
  color: ${props => props.variant === 'primary' ? 'white' : '#333'};
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  
  &:hover {
    background-color: ${props => props.variant === 'primary' ? '#0056b3' : '#f8f9fa'};
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

const HelpText = styled.p`
  font-size: 13px;
  color: #666;
  margin: 4px 0 0 0;
`;

interface SaveDashboardDialogProps {
  onClose: () => void;
  onSave: (height?: string) => void;
  currentHeight?: string | null;
}

const SaveDashboardDialog: React.FC<SaveDashboardDialogProps> = ({
  onClose,
  onSave,
  currentHeight
}) => {
  const [useFixedHeight, setUseFixedHeight] = useState(!!currentHeight);
  const [heightValue, setHeightValue] = useState(currentHeight || '800px');

  const handleSave = () => {
    if (useFixedHeight) {
      // Validate height value
      if (!heightValue.trim()) {
        alert('Please enter a valid height value');
        return;
      }
      // Ensure the value has a unit (px, vh, etc.)
      let normalizedHeight = heightValue.trim();
      if (!/\d+(?:px|vh|%|em|rem)$/.test(normalizedHeight)) {
        // Default to px if no unit specified
        if (/^\d+$/.test(normalizedHeight)) {
          normalizedHeight = normalizedHeight + 'px';
        } else {
          alert('Please enter a valid height value with units (e.g., 800px, 50vh, 100%)');
          return;
        }
      }
      onSave(normalizedHeight);
    } else {
      // Use full viewport height (no fixed height)
      onSave(undefined);
    }
  };

  return (
    <DialogOverlay onClick={onClose}>
      <DialogContainer onClick={(e) => e.stopPropagation()}>
        <DialogTitle>Save Dashboard</DialogTitle>
        
        <FormGroup>
          <CheckboxContainer>
            <Checkbox
              id="useFixedHeight"
              checked={useFixedHeight}
              onChange={(e) => setUseFixedHeight(e.target.checked)}
            />
            <Label htmlFor="useFixedHeight">
              Use fixed height instead of full screen
            </Label>
          </CheckboxContainer>
          <HelpText>
            {useFixedHeight 
              ? 'Dashboard will have a fixed height and may scroll if content is larger'
              : 'Dashboard will occupy the full viewport height (default behavior)'
            }
          </HelpText>
        </FormGroup>

        {useFixedHeight && (
          <FormGroup>
            <Label htmlFor="heightInput">Dashboard Height:</Label>
            <Input
              id="heightInput"
              type="text"
              value={heightValue}
              onChange={(e) => setHeightValue(e.target.value)}
              placeholder="e.g., 800px, 50vh, 100%"
            />
            <HelpText>
              Specify height with units: px, vh (viewport height %), %, em, or rem
            </HelpText>
          </FormGroup>
        )}

        <ButtonContainer>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSave}>
            Save Dashboard
          </Button>
        </ButtonContainer>
      </DialogContainer>
    </DialogOverlay>
  );
};

export default SaveDashboardDialog;