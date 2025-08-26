"use client";
// src/components/ModelPanel.js
import React, { useState, useEffect } from 'react';

const ModelPanel = () => {
  const [models, setModels] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate fetching available 3D models from the public/models folder
    const fetchModels = async () => {
      try {
        // For now, we'll hardcode the known models, but in a real app you might fetch this from an API
        const availableModels = [
          {
            id: 'camp_site',
            name: 'Camp Site',
            filename: 'camp_site.glb',
            description: 'A detailed camping site with tents and fire',
            category: 'Environment',
            size: '2.1 MB',
            icon: '🏕️'
          },
          {
            id: 'pencil',
            name: 'Pencil',
            filename: 'pencil.glb',
            description: 'A simple pencil model for AR placement',
            category: 'Objects',
            size: '512 KB',
            icon: '✏️'
          }
        ];

        // Simulate network delay
        setTimeout(() => {
          setModels(availableModels);
          setLoading(false);
        }, 1000);
      } catch (error) {
        console.error('Failed to fetch models:', error);
        setLoading(false);
      }
    };

    fetchModels();
  }, []);

  const getModelUrl = (filename) => {
    return `http:// 192.168.1.4:3000/models/${filename}`;
  };

  const handleModelClick = (model) => {
    const url = getModelUrl(model.filename);
    console.log('Model selected:', model.name, 'URL:', url);
    
    // Copy URL to clipboard for easy use
    navigator.clipboard.writeText(url).then(() => {
      console.log('Model URL copied to clipboard:', url);
    }).catch(err => {
      console.error('Failed to copy URL:', err);
    });
  };

  const handleDragStart = (e, model) => {
    const dragData = {
      type: 'model',
      model: model,
      url: getModelUrl(model.filename)
    };
    e.dataTransfer.setData('application/json', JSON.stringify(dragData));
    e.dataTransfer.effectAllowed = 'copy';
    
    console.log('🎯 Started dragging model:', model.name);
  };

  if (loading) {
    return (
      <div style={{
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#666'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '32px', marginBottom: '16px' }}>⏳</div>
          <p>Loading models...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      flex: 1,
      overflow: 'auto',
      padding: '10px'
    }}>
      {models.length === 0 ? (
        <div style={{
          textAlign: 'center',
          color: '#666',
          padding: '40px 20px',
          fontSize: '14px'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>📦</div>
          <p>No models available</p>
          <p style={{ fontSize: '12px', marginTop: '8px' }}>
            Add .glb files to public/models/
          </p>
        </div>
      ) : (
        models.map((model) => (
          <div
            key={model.id}
            draggable={true}
            style={{
              backgroundColor: '#3a3a3a',
              border: '1px solid #555',
              borderRadius: '8px',
              padding: '16px',
              marginBottom: '12px',
              boxShadow: '0 2px 4px rgba(0,0,0,0.3)',
              cursor: 'grab',
              transition: 'all 0.2s ease',
              userSelect: 'none'
            }}
            onClick={() => handleModelClick(model)}
            onDragStart={(e) => handleDragStart(e, model)}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = '#4a4a4a';
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.4)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = '#3a3a3a';
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.3)';
            }}
          >
            {/* Header with icon and name */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              marginBottom: '12px'
            }}>
              <span style={{ 
                fontSize: '32px', 
                marginRight: '12px' 
              }}>
                {model.icon}
              </span>
              <div style={{ flex: 1 }}>
                <h3 style={{
                  margin: 0,
                  color: '#fff',
                  fontSize: '16px',
                  fontWeight: 'bold'
                }}>
                  {model.name}
                </h3>
                <p style={{
                  margin: 0,
                  color: '#aaa',
                  fontSize: '12px'
                }}>
                  {model.category} • {model.size}
                </p>
              </div>
            </div>

            {/* Description */}
            <div style={{
              color: '#ccc',
              fontSize: '14px',
              marginBottom: '12px',
              lineHeight: '1.4'
            }}>
              {model.description}
            </div>

            {/* File info */}
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
                Filename:
              </div>
              <div style={{
                color: '#fff',
                fontSize: '14px',
                fontFamily: 'monospace'
              }}>
                {model.filename}
              </div>
            </div>

            {/* URL preview */}
            <div style={{
              backgroundColor: '#1a1a1a',
              borderRadius: '4px',
              padding: '8px',
              border: '1px solid #444'
            }}>
              <div style={{
                color: '#888',
                fontSize: '11px',
                marginBottom: '2px'
              }}>
                URL:
              </div>
              <div style={{
                color: '#4CAF50',
                fontSize: '12px',
                fontFamily: 'monospace',
                wordBreak: 'break-all'
              }}>
                {getModelUrl(model.filename)}
              </div>
            </div>

            {/* Click hint */}
            <div style={{
              color: '#666',
              fontSize: '11px',
              textAlign: 'center',
              marginTop: '8px',
              fontStyle: 'italic'
            }}>
              Click to copy URL
            </div>
          </div>
        ))
      )}
    </div>
  );
};

export default ModelPanel;