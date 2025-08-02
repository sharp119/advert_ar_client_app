"use client";

// src/components/ThreeDGraphViewer.js
import React, { useRef, useEffect, useState, useCallback } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'; // Import OrbitControls

const WEBSOCKET_URL = 'ws://localhost:8080'; // Your Node.js WebSocket server URL

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
        // console.log('Received AR data on frontend:', data);

        // Update device mesh position
        if (deviceMeshRef.current) {
          // ARCore uses Y-up. Three.js typically Z-up.
          // You might need to swap coordinates or apply a rotation to align.
          // For simple plotting, direct mapping might work, but visual orientation might be off.
          // Let's assume ARCore's Y is Three.js's Y for simplicity in plotting position.
          // Position: (X, Y, Z)
          deviceMeshRef.current.position.set(data.devicePositionX, data.devicePositionY, data.devicePositionZ);

          // Rotation: (X, Y, Z, W) - Three.js Quaternions are (x, y, z, w)
          deviceMeshRef.current.quaternion.set(data.deviceRotationX, data.deviceRotationY, data.deviceRotationZ, data.deviceRotationW);

          // Add current position to path points
          setPathPoints(prevPoints => [...prevPoints, new THREE.Vector3(data.devicePositionX, data.devicePositionY, data.devicePositionZ)]);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
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
    };
  }, []); // Empty dependency array means this effect runs once on mount

  // Effect to update the path line whenever pathPoints changes
  useEffect(() => {
    updatePathLine();
  }, [pathPoints, updatePathLine]);

  return (
    <div
      ref={mountRef}
      style={{ width: '100vw', height: '100vh', overflow: 'hidden' }} // Full viewport size
    />
  );
};

export default ThreeDGraphViewer;