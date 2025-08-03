"use client";
// src/components/NodePanel.js
import React, { useState, useEffect, useRef } from 'react';

const WEBSOCKET_URL = 'ws://localhost:8080';

const NodePanel = () => {
  const [anchorNodes, setAnchorNodes] = useState(new Map());
  const [dragOverNode, setDragOverNode] = useState(null);
  const wsRef = useRef(null);

  // Send mapping to API endpoint
  const addMapping = async (nodeId, modelUrl, modelName) => {
    try {
      console.log('🎯 Adding mapping:', { nodeId, modelUrl, modelName });
      
      const response = await fetch('/api/mappings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          nodeId: nodeId,
          modelUrl: modelUrl,
          modelName: modelName
        })
      });

      if (response.ok) {
        console.log('✅ Mapping added successfully');
      } else {
        console.error('❌ Failed to add mapping:', response.statusText);
      }
    } catch (error) {
      console.error('❌ Error adding mapping:', error);
    }
  };

  const handleDragOver = (e, node) => {
    if (node.nodeType === 'Link') {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'copy';
    }
  };

  const handleDragEnter = (e, node) => {
    if (node.nodeType === 'Link') {
      e.preventDefault();
      setDragOverNode(node.nodeId);
    }
  };

  const handleDragLeave = (e, node) => {
    if (node.nodeType === 'Link') {
      setDragOverNode(null);
    }
  };

  const handleDrop = (e, node) => {
    e.preventDefault();
    setDragOverNode(null);

    if (node.nodeType !== 'Link') {
      console.log('❌ Can only drop models on Link-type nodes');
      return;
    }

    try {
      const dragData = JSON.parse(e.dataTransfer.getData('application/json'));
      
      if (dragData.type === 'model') {
        console.log(`🎯 Dropped ${dragData.model.name} on Link node ${node.nodeId}`);
        
        // Add mapping to API
        addMapping(node.nodeId, dragData.url, dragData.model.name);
      }
    } catch (error) {
      console.error('❌ Error handling drop:', error);
    }
  };

  useEffect(() => {
    // WebSocket connection to listen for anchor node data
    wsRef.current = new WebSocket(WEBSOCKET_URL);

    wsRef.current.onopen = () => {
      console.log('NodePanel WebSocket connected!');
    };

    wsRef.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        // Only handle nodeAnchor messages
        if (data.type === 'nodeAnchor') {
          console.log('NodePanel received anchor data:', data);
          
          setAnchorNodes(prevNodes => {
            const newNodes = new Map(prevNodes);
            newNodes.set(data.nodeId, {
              nodeId: data.nodeId,
              nodeType: data.nodeType,
              anchorPositionX: data.anchorPositionX,
              anchorPositionY: data.anchorPositionY,
              anchorPositionZ: data.anchorPositionZ,
              timestamp: data.timestamp
            });
            return newNodes;
          });
        }
      } catch (error) {
        console.error('NodePanel: Failed to parse WebSocket message:', error);
      }
    };

    wsRef.current.onclose = () => {
      console.log('NodePanel WebSocket disconnected.');
    };

    wsRef.current.onerror = (error) => {
      console.error('NodePanel WebSocket error:', error);
    };

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  const getNodeTypeIcon = (nodeType) => {
    switch (nodeType) {
      case 'Sphere': return '🔵';
      case 'Cube': return '🟦';
      case 'Cylinder': return '🥤';
      case 'Link': return '🔗';
      case 'CloudAnchor': return '☁️';
      case 'Andy': return '🤖';
      case 'Video': return '📹';
      case 'Measure': return '📏';
      case 'Layout': return '📐';
      default: return '📍';
    }
  };

  const formatPosition = (x, y, z) => {
    return `(${x?.toFixed(2) || '0.00'}, ${y?.toFixed(2) || '0.00'}, ${z?.toFixed(2) || '0.00'})`;
  };

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString();
  };

  // Convert Map to Array and sort by timestamp (newest first)
  const nodeArray = Array.from(anchorNodes.values()).sort((a, b) => b.timestamp - a.timestamp);

  return (
    <div style={{
      flex: 1,
      overflow: 'auto',
      padding: '10px'
    }}>
      {nodeArray.length === 0 ? (
        <div style={{
          textAlign: 'center',
          color: '#666',
          padding: '40px 20px',
          fontSize: '14px'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎯</div>
          <p>No anchor nodes yet</p>
          <p style={{ fontSize: '12px', marginTop: '8px' }}>
            Place AR objects to see them here
          </p>
        </div>
      ) : (
        nodeArray.map((node) => {
          const isLinkNode = node.nodeType === 'Link';
          const isDraggedOver = dragOverNode === node.nodeId;
          
          return (
            <div
              key={node.nodeId}
              style={{
                backgroundColor: isDraggedOver ? '#4CAF50' : '#3a3a3a',
                border: isLinkNode ? (isDraggedOver ? '2px solid #81C784' : '2px solid #4CAF50') : '1px solid #555',
                borderRadius: '8px',
                padding: '16px',
                marginBottom: '12px',
                boxShadow: isDraggedOver ? '0 4px 12px rgba(76,175,80,0.4)' : '0 2px 4px rgba(0,0,0,0.3)',
                transition: 'all 0.2s ease',
                cursor: isLinkNode ? 'pointer' : 'default',
                position: 'relative'
              }}
              onDragOver={(e) => handleDragOver(e, node)}
              onDragEnter={(e) => handleDragEnter(e, node)}
              onDragLeave={(e) => handleDragLeave(e, node)}
              onDrop={(e) => handleDrop(e, node)}
              onMouseEnter={(e) => {
                if (!isDraggedOver) {
                  e.currentTarget.style.backgroundColor = '#4a4a4a';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }
              }}
              onMouseLeave={(e) => {
                if (!isDraggedOver) {
                  e.currentTarget.style.backgroundColor = '#3a3a3a';
                  e.currentTarget.style.transform = 'translateY(0)';
                }
              }}
            >
            {/* Header with icon and type */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              marginBottom: '12px'
            }}>
              <span style={{ 
                fontSize: '24px', 
                marginRight: '12px' 
              }}>
                {getNodeTypeIcon(node.nodeType)}
              </span>
              <div>
                <h3 style={{
                  margin: 0,
                  color: '#fff',
                  fontSize: '16px',
                  fontWeight: 'bold'
                }}>
                  {node.nodeType}
                </h3>
                <p style={{
                  margin: 0,
                  color: '#aaa',
                  fontSize: '12px'
                }}>
                  ID: {node.nodeId.substring(0, 8)}...
                </p>
              </div>
            </div>

            {/* Position info */}
            <div style={{
              backgroundColor: '#2a2a2a',
              borderRadius: '4px',
              padding: '8px',
              marginBottom: '8px'
            }}>
              <div style={{
                color: '#ccc',
                fontSize: '12px',
                marginBottom: '4px'
              }}>
                Position:
              </div>
              <div style={{
                color: '#fff',
                fontSize: '14px',
                fontFamily: 'monospace'
              }}>
                {formatPosition(node.anchorPositionX, node.anchorPositionY, node.anchorPositionZ)}
              </div>
            </div>

            {/* Timestamp */}
            <div style={{
              color: '#888',
              fontSize: '11px',
              textAlign: 'right'
            }}>
              Created: {formatTimestamp(node.timestamp)}
            </div>

            {/* Drop zone indicator for Link nodes */}
            {isLinkNode && (
              <div style={{
                position: 'absolute',
                top: '8px',
                right: '8px',
                backgroundColor: isDraggedOver ? '#81C784' : '#4CAF50',
                color: 'white',
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '10px',
                fontWeight: 'bold'
              }}>
                {isDraggedOver ? 'DROP HERE' : 'DRAG & DROP'}
              </div>
            )}
          </div>
        );
        })
      )}
    </div>
  );
};

export default NodePanel;