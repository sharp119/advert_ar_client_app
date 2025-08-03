"use client";

// src/components/ThreeDGraphViewer.js
import React, { useRef, useEffect, useState, useCallback } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'; // Import OrbitControls

const WEBSOCKET_URL = 'ws://localhost:8080'; // Your Node.js WebSocket server URL
const API_URL = 'http://localhost:3001'; // HTTP API server URL

const ThreeDGraphViewer = () => {
  const mountRef = useRef(null); // Reference to the DOM element to mount the Three.js canvas
  const sceneRef = useRef(null); // Reference to the Three.js scene
  const cameraRef = useRef(null); // Reference to the Three.js camera
  const rendererRef = useRef(null); // Reference to the Three.js renderer
  const controlsRef = useRef(null); // Reference to OrbitControls
  const wsRef = useRef(null); // Reference to WebSocket connection

  // State to store the path points for the device's movement
  const [pathPoints, setPathPoints] = useState([]);
  const pathLineRef = useRef(null); // Reference to the Three.js line representing the path

  // Reference to the 3D object representing the device
  const deviceMeshRef = useRef(null);
  
  // State and references for anchor points
  const [anchorPoints, setAnchorPoints] = useState(new Map());
  const anchorMeshesRef = useRef(new Map());

  // State for location and venue data
  const [locationData, setLocationData] = useState(null);

  // Function to update the path line in the scene
  const updatePathLine = useCallback(() => {
    if (pathLineRef.current) {
      sceneRef.current.remove(pathLineRef.current); // Remove old line
    }
    if (pathPoints.length > 1) {
      const geometry = new THREE.BufferGeometry().setFromPoints(pathPoints);
      const material = new THREE.LineBasicMaterial({ color: 0x00ff00, linewidth: 2 }); // Green line
      pathLineRef.current = new THREE.Line(geometry, material);
      sceneRef.current.add(pathLineRef.current);
    }
  }, [pathPoints]);

  // Function to create or update anchor points in the scene
  const updateAnchorPoints = useCallback(() => {
    // Remove old anchor meshes
    anchorMeshesRef.current.forEach((mesh, nodeId) => {
      if (!anchorPoints.has(nodeId)) {
        sceneRef.current.remove(mesh);
        mesh.geometry.dispose();
        mesh.material.dispose();
        anchorMeshesRef.current.delete(nodeId);
      }
    });

    // Add or update anchor meshes
    anchorPoints.forEach((anchorData, nodeId) => {
      if (!anchorMeshesRef.current.has(nodeId)) {
        // Create new anchor visualization - BIGGER blue sphere
        const geometry = new THREE.SphereGeometry(0.15, 16, 16); // Bigger blue sphere (0.15 instead of 0.05)
        const material = new THREE.MeshBasicMaterial({ 
          color: 0x0077ff, // Blue color
          transparent: true,
          opacity: 0.9
        });
        const anchorMesh = new THREE.Mesh(geometry, material);
        
        // Position the anchor
        anchorMesh.position.set(
          anchorData.anchorPositionX,
          anchorData.anchorPositionY,
          anchorData.anchorPositionZ
        );
        
        // Add text label for node type
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        canvas.width = 256;
        canvas.height = 64;
        context.fillStyle = '#ffffff';
        context.fillRect(0, 0, canvas.width, canvas.height);
        context.fillStyle = '#000000';
        context.font = '16px Arial';
        context.textAlign = 'center';
        context.fillText(anchorData.nodeType, canvas.width / 2, canvas.height / 2);
        
        const texture = new THREE.CanvasTexture(canvas);
        const labelMaterial = new THREE.SpriteMaterial({ map: texture });
        const label = new THREE.Sprite(labelMaterial);
        label.scale.set(0.5, 0.125, 1);
        label.position.set(0, 0.1, 0); // Slightly above the sphere
        anchorMesh.add(label);
        
        sceneRef.current.add(anchorMesh);
        anchorMeshesRef.current.set(nodeId, anchorMesh);
        
        console.log(`✅ Added anchor visualization: ${nodeId} (${anchorData.nodeType}) at position (${anchorData.anchorPositionX?.toFixed(3)}, ${anchorData.anchorPositionY?.toFixed(3)}, ${anchorData.anchorPositionZ?.toFixed(3)})`);
      } else {
        console.log(`🔄 Anchor ${nodeId} already exists, skipping creation`);
      }
    });
    console.log(`🎯 updateAnchorPoints completed. Total anchors: ${anchorPoints.size}, Total meshes: ${anchorMeshesRef.current.size}`);
  }, [anchorPoints]);

  useEffect(() => {
    // --- Scene Setup ---
    const currentMount = mountRef.current;
    if (!currentMount) return;

    // 1. Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;
    scene.background = new THREE.Color(0x1a1a1a); // Dark background

    // 2. Camera
    // Adjusted FOV and aspect ratio for better viewing
    const camera = new THREE.PerspectiveCamera(75, currentMount.clientWidth / currentMount.clientHeight, 0.1, 1000);
    camera.position.set(0, 5, 10); // Initial camera position (X, Y, Z)
    cameraRef.current = camera;

    // 3. Renderer
    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(currentMount.clientWidth, currentMount.clientHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    currentMount.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // 4. OrbitControls for interaction (zoom, pan, rotate)
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true; // Smooth camera movements
    controls.dampingFactor = 0.05;
    controls.screenSpacePanning = false; // Prevents panning in screen space
    controls.minDistance = 0.1; // Minimum zoom distance
    controls.maxDistance = 50; // Maximum zoom distance
    controlsRef.current = controls;

    // 5. Add Grid Helper for visual reference
    const gridHelper = new THREE.GridHelper(10, 10, 0x444444, 0x444444); // Size, Divisions, Center Line Color, Grid Color
    scene.add(gridHelper);

    // 6. Add Axes Helper for orientation reference (X=red, Y=green, Z=blue)
    const axesHelper = new THREE.AxesHelper(2); // Size
    scene.add(axesHelper);

    // 7. Create a simple mesh to represent the device (e.g., a small cube)
    const deviceGeometry = new THREE.BoxGeometry(0.1, 0.1, 0.2); // Small box to represent phone
    const deviceMaterial = new THREE.MeshBasicMaterial({ color: 0xff0000 }); // Red color
    const deviceMesh = new THREE.Mesh(deviceGeometry, deviceMaterial);
    scene.add(deviceMesh);
    deviceMeshRef.current = deviceMesh;

    // --- Animation Loop ---
    const animate = () => {
      requestAnimationFrame(animate);
      controls.update(); // Only required if controls.enableDamping or controls.autoRotate are set to true
      renderer.render(scene, camera);
    };
    animate();

    // --- Handle Window Resizing ---
    const handleResize = () => {
      camera.aspect = currentMount.clientWidth / currentMount.clientHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(currentMount.clientWidth, currentMount.clientHeight);
    };
    window.addEventListener('resize', handleResize);

    // --- WebSocket Connection ---
    wsRef.current = new WebSocket(WEBSOCKET_URL);

    wsRef.current.onopen = () => {
      console.log('WebSocket connected from frontend!');
    };

    wsRef.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        // console.log('🔗 Received WebSocket data:', data); // DEBUG: Log all incoming data

        // Handle different types of data
        switch (data.type) {
          case 'devicePose':
            // console.log(`📱 Device pose update: Position(${data.devicePositionX?.toFixed(3)}, ${data.devicePositionY?.toFixed(3)}, ${data.devicePositionZ?.toFixed(3)})`);
            // Update device mesh position and path
            if (deviceMeshRef.current) {
              // Position: (X, Y, Z)
              deviceMeshRef.current.position.set(data.devicePositionX, data.devicePositionY, data.devicePositionZ);
              // Rotation: (X, Y, Z, W) - Three.js Quaternions are (x, y, z, w)
              deviceMeshRef.current.quaternion.set(data.deviceRotationX, data.deviceRotationY, data.deviceRotationZ, data.deviceRotationW);
              // Add current position to path points
              setPathPoints(prevPoints => [...prevPoints, new THREE.Vector3(data.devicePositionX, data.devicePositionY, data.devicePositionZ)]);
            }
            break;

          case 'nodeAnchor':
            // Handle anchor data for placed AR objects
            console.log(`🎯 ANCHOR DATA RECEIVED:`, {
              nodeId: data.nodeId,
              nodeType: data.nodeType,
              position: `(${data.anchorPositionX?.toFixed(3)}, ${data.anchorPositionY?.toFixed(3)}, ${data.anchorPositionZ?.toFixed(3)})`,
              timestamp: new Date(data.timestamp).toLocaleTimeString()
            });
            
            setAnchorPoints(prevAnchors => {
              const newAnchors = new Map(prevAnchors);
              newAnchors.set(data.nodeId, {
                nodeId: data.nodeId,
                nodeType: data.nodeType,
                anchorPositionX: data.anchorPositionX,
                anchorPositionY: data.anchorPositionY,
                anchorPositionZ: data.anchorPositionZ,
                anchorRotationX: data.anchorRotationX,
                anchorRotationY: data.anchorRotationY,
                anchorRotationZ: data.anchorRotationZ,
                anchorRotationW: data.anchorRotationW,
                timestamp: data.timestamp
              });
              console.log(`📊 Total anchors now: ${newAnchors.size}`);
              return newAnchors;
            });
            break;

          default:
            console.log(`❓ Unknown message type: ${data.type || 'undefined'}`);
            // Handle legacy data format (backward compatibility)
            if (data.devicePositionX !== undefined && deviceMeshRef.current) {
              console.log('📱 Legacy device pose data');
              deviceMeshRef.current.position.set(data.devicePositionX, data.devicePositionY, data.devicePositionZ);
              deviceMeshRef.current.quaternion.set(data.deviceRotationX, data.deviceRotationY, data.deviceRotationZ, data.deviceRotationW);
              setPathPoints(prevPoints => [...prevPoints, new THREE.Vector3(data.devicePositionX, data.devicePositionY, data.devicePositionZ)]);
            }
            break;
        }
      } catch (error) {
        console.error('❌ Failed to parse WebSocket message:', error, 'Raw data:', event.data);
      }
    };

    wsRef.current.onclose = () => {
      console.log('WebSocket disconnected from frontend.');
    };

    wsRef.current.onerror = (error) => {
      console.error('WebSocket error on frontend:', error);
    };

    // --- Cleanup ---
    return () => {
      window.removeEventListener('resize', handleResize);
      if (wsRef.current) {
        wsRef.current.close(); // Close WebSocket on unmount
      }
      if (currentMount) {
        currentMount.removeChild(renderer.domElement); // Remove canvas from DOM
      }
      // Dispose Three.js resources to prevent memory leaks
      renderer.dispose();
      controls.dispose();
      scene.clear(); // Clear all objects from the scene
      // Dispose geometries and materials if created directly (not from loaded models)
      deviceGeometry.dispose();
      deviceMaterial.dispose();
      if (pathLineRef.current) {
        pathLineRef.current.geometry.dispose();
        pathLineRef.current.material.dispose();
      }
      // Dispose anchor meshes
      anchorMeshesRef.current.forEach((mesh) => {
        mesh.geometry.dispose();
        mesh.material.dispose();
        if (mesh.children.length > 0) {
          mesh.children.forEach(child => {
            if (child.material) child.material.dispose();
            if (child.geometry) child.geometry.dispose();
          });
        }
      });
      anchorMeshesRef.current.clear();
    };
  }, []); // Empty dependency array means this effect runs once on mount

  // Effect to update the path line whenever pathPoints changes
  useEffect(() => {
    updatePathLine();
  }, [pathPoints, updatePathLine]);

  // Effect to update anchor points whenever anchorPoints changes
  useEffect(() => {
    updateAnchorPoints();
  }, [anchorPoints, updateAnchorPoints]);

  // Function to fetch location data from API
  const fetchLocationData = useCallback(async () => {
    try {
      const response = await fetch(`${API_URL}/api/location`);
      if (response.ok) {
        const result = await response.json();
        if (result.success && result.data) {
          setLocationData(result.data);
        }
      }
    } catch (error) {
      console.error('Failed to fetch location data:', error);
    }
  }, []);

  // Effect to fetch location data periodically
  useEffect(() => {
    // Fetch immediately
    fetchLocationData();
    
    // Then fetch every 5 seconds
    const interval = setInterval(fetchLocationData, 5000);
    
    return () => clearInterval(interval);
  }, [fetchLocationData]);

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* 3D Canvas */}
      <div
        ref={mountRef}
        style={{ width: '100%', height: '100%' }}
      />
      
      {/* Venue Display Overlay - Positioned for center panel */}
      {locationData && (
        <div
          style={{
            position: 'absolute',
            top: '20px',
            left: '20px',
            right: '20px',
            backgroundColor: 'rgba(0, 0, 0, 0.9)',
            color: 'white',
            padding: '12px 16px',
            borderRadius: '8px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            zIndex: 1000,
            fontFamily: 'Arial, sans-serif',
            fontSize: '14px',
            border: '1px solid #444'
          }}
        >
          <div>
            <h2 style={{ margin: '0 0 4px 0', fontSize: '18px', fontWeight: 'bold' }}>
              📍 {locationData.venueName}
            </h2>
            <p style={{ margin: 0, fontSize: '12px', opacity: 0.8 }}>
              {locationData.latitude.toFixed(6)}, {locationData.longitude.toFixed(6)}
            </p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p style={{ margin: '0 0 2px 0', fontSize: '11px', opacity: 0.6 }}>
              Accuracy: {locationData.accuracy.toFixed(1)}m
            </p>
            <p style={{ margin: 0, fontSize: '11px', opacity: 0.6 }}>
              {new Date(locationData.timestamp).toLocaleTimeString()}
            </p>
          </div>
        </div>
      )}

      {/* Statistics overlay */}
      <div
        style={{
          position: 'absolute',
          bottom: '20px',
          left: '20px',
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          color: 'white',
          padding: '12px',
          borderRadius: '6px',
          fontSize: '12px',
          fontFamily: 'monospace',
          border: '1px solid #444',
          zIndex: 1000
        }}
      >
        <div style={{ marginBottom: '4px' }}>
          📊 Stats
        </div>
        <div style={{ color: '#4CAF50' }}>
          Path Points: {pathPoints.length}
        </div>
        <div style={{ color: '#2196F3' }}>
          Anchors: {anchorPoints.size}
        </div>
      </div>

      {/* Controls info */}
      <div
        style={{
          position: 'absolute',
          bottom: '20px',
          right: '20px',
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          color: 'white',
          padding: '12px',
          borderRadius: '6px',
          fontSize: '11px',
          fontFamily: 'Arial, sans-serif',
          border: '1px solid #444',
          zIndex: 1000,
          maxWidth: '200px'
        }}
      >
        <div style={{ marginBottom: '6px', fontWeight: 'bold' }}>
          🎮 Controls
        </div>
        <div style={{ opacity: 0.8, lineHeight: 1.3 }}>
          • Mouse: Rotate view<br/>
          • Scroll: Zoom in/out<br/>
          • Drag: Pan camera
        </div>
      </div>
    </div>
  );
};

export default ThreeDGraphViewer;