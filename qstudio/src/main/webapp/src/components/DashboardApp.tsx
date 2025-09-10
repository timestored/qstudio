import React, { Component } from 'react';
import { Layout, Model, TabNode, TabSetNode, IJsonModel } from 'flexlayout-react';
import 'flexlayout-react/style/light.css';
import FlexContainer from './FlexContainer';
import SaveDashboardDialog from '../dialogs/SaveDashboardDialog';

interface DashboardAppState {
  model: Model;
  modelChangesSaved: boolean;
  showSaveDialog: boolean;
  dashboardHeight: string | null; // Track the configured dashboard height
}

interface DashboardAppProps {
  topMargin?: string;
  rightMargin?: string;
}

class DashboardApp extends Component<DashboardAppProps, DashboardAppState> {
  private layoutRef = React.createRef<Layout>();

  constructor(props: DashboardAppProps) {
    super(props);
    
    // Default layout configuration
    const json: IJsonModel = {
      global: {
        tabEnableClose: false,
        tabEnableFloat: true,
        tabSetMinWidth: 100,
        tabSetMinHeight: 100,
      },
      borders: [],
      layout: {
        type: "row",
        weight: 100,
        children: [
          {
            type: "tabset",
            weight: 50,
            children: [
              {
                type: "tab",
                name: "Chart 1",
                component: "chart",
              }
            ]
          },
          {
            type: "tabset", 
            weight: 50,
            children: [
              {
                type: "tab",
                name: "Chart 2",
                component: "chart",
              }
            ]
          }
        ]
      }
    };

    this.state = {
      model: Model.fromJson(json),
      modelChangesSaved: true,
      showSaveDialog: false,
      dashboardHeight: null, // Initially no fixed height (uses full viewport)
    };
  }

  factory = (node: TabNode): React.ReactNode => {
    const component = node.getComponent();
    
    if (component === "chart") {
      return (
        <div style={{ padding: '10px' }}>
          <h3>{node.getName()}</h3>
          <p>Chart component placeholder</p>
        </div>
      );
    }
    
    return <div>Unknown component: {component}</div>;
  }

  handleAction = (action: any): any => {
    // Handle layout actions
    return action;
  }

  onRenderTabSet = (tabSetNode: TabSetNode | any, renderValues: any): void => {
    // Custom tab set rendering if needed
    // This method doesn't need to return anything, it modifies renderValues
  }

  handleSaveDashboard = () => {
    this.setState({ showSaveDialog: true });
  }

  handleSaveDialogClose = () => {
    this.setState({ showSaveDialog: false });
  }

  handleSaveWithHeight = (height?: string) => {
    this.setState({ 
      dashboardHeight: height || null,
      showSaveDialog: false,
      modelChangesSaved: true 
    });
  }

  render() {
    const { topMargin, rightMargin } = this.props;
    const { model, showSaveDialog, dashboardHeight } = this.state;

    return (
      <>
        <FlexContainer 
          topMargin={topMargin} 
          rightMargin={rightMargin} 
          fixedHeight={dashboardHeight || undefined}
          id="dashScreenshotContainer"
        >
          <div id="FlexContainerInner">
            <Layout 
              ref={this.layoutRef} 
              model={model} 
              factory={this.factory}
              onAction={this.handleAction}  
              onRenderTabSet={this.onRenderTabSet} 
              font={{size:"16px"}}
              //onModelChange={() => this.setState({modelChangesSaved:false})} Could listen to detect unsaved changes
            />
          </div>
        </FlexContainer>

        {/* Save button for testing */}
        <button 
          onClick={this.handleSaveDashboard}
          style={{
            position: 'fixed',
            top: '10px',
            right: '10px',
            padding: '10px 20px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Save Dashboard
        </button>

        {showSaveDialog && (
          <SaveDashboardDialog
            onClose={this.handleSaveDialogClose}
            onSave={this.handleSaveWithHeight}
            currentHeight={dashboardHeight}
          />
        )}
      </>
    );
  }
}

export default DashboardApp;