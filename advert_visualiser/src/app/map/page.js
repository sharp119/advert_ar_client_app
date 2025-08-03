// src/app/map/page.js
"use client";
import ThreeDGraphViewer from '@/components/ThreeDGraphViewer';
import NodePanel from '@/components/NodePanel';
import ModelPanel from '@/components/ModelPanel';

export default function MapPage() {
  return (
    <div style={{ 
      display: 'flex', 
      width: '100vw', 
      height: '100vh', 
      fontFamily: 'Arial, sans-serif',
      backgroundColor: '#f0f0f0'
    }}>
      {/* Left Panel - Node Cards */}
      <div style={{ 
        width: '300px', 
        backgroundColor: '#2a2a2a', 
        borderRight: '2px solid #444',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{
          padding: '20px',
          borderBottom: '1px solid #444',
          backgroundColor: '#1a1a1a'
        }}>
          <h2 style={{ 
            margin: 0, 
            color: '#fff', 
            fontSize: '18px',
            fontWeight: 'bold'
          }}>
            🎯 AR Nodes
          </h2>
          <p style={{ 
            margin: '8px 0 0 0', 
            color: '#aaa', 
            fontSize: '14px' 
          }}>
            Placed anchor points
          </p>
        </div>
        <NodePanel />
      </div>

      {/* Center Panel - 3D Visualizer */}
      <div style={{ 
        flex: 1, 
        position: 'relative',
        backgroundColor: '#1a1a1a'
      }}>
        <ThreeDGraphViewer />
      </div>

      {/* Right Panel - 3D Models */}
      <div style={{ 
        width: '300px', 
        backgroundColor: '#2a2a2a', 
        borderLeft: '2px solid #444',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{
          padding: '20px',
          borderBottom: '1px solid #444',
          backgroundColor: '#1a1a1a'
        }}>
          <h2 style={{ 
            margin: 0, 
            color: '#fff', 
            fontSize: '18px',
            fontWeight: 'bold'
          }}>
            🎲 3D Models
          </h2>
          <p style={{ 
            margin: '8px 0 0 0', 
            color: '#aaa', 
            fontSize: '14px' 
          }}>
            Available assets
          </p>
        </div>
        <ModelPanel />
      </div>
    </div>
  );
}