import styled from 'styled-components';

interface FlexContainerProps {
  topMargin?: string;
  rightMargin?: string;
  fixedHeight?: string; // New prop for fixed height support
}

const FlexContainer = styled.div.withConfig({
  shouldForwardProp: (prop) => !['topMargin', 'rightMargin', 'fixedHeight'].includes(prop),
})<FlexContainerProps>`
    position: relative;
    width: ${props => 'calc(100vw - ' + ("rightMargin" in props ? props.rightMargin : '0px') + ')'};
    height: ${props => {
      // If fixedHeight is specified, use that instead of viewport height
      if (props.fixedHeight) {
        return props.fixedHeight;
      }
      // Original behavior - full viewport minus margins
      return 'calc(100vh - (' + ("topMargin" in props ? props.topMargin : '37px') + '))';
    }};
`;

export default FlexContainer;