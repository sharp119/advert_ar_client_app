// // Import the WebSocket library
// const WebSocket = require('ws');

// // Define the port for the WebSocket server
// // This port must be accessible from your Android device/emulator.
// // For emulator, use 10.0.2.2:PORT. For physical device, use your machine's local IP:PORT.
// const PORT = 8080;

// // Create a new WebSocket server instance
// const wss = new WebSocket.Server({ port: PORT });

// console.log(`🚀 WebSocket server started on ws://localhost:${PORT}`);
// console.log('Waiting for AR client connections...');

// // Event listener for new WebSocket connections
// wss.on('connection', ws => {
//     console.log('\n✨ Client connected!');
//     console.log(`Total connected clients: ${wss.clients.size}`);

//     /**
//      * Event listener for messages received from a client.
//      * The AR client will send JSON strings containing pose data.
//      */
//     ws.on('message', message => {
//         try {
//             // Attempt to parse the incoming message as a JSON object
//             const data = JSON.parse(message);

//             // Log the received data to the console
//             // You can customize this logging format as needed
//             console.log(`\n--- Received AR Data ---`);
//             console.log(`Timestamp: ${new Date(data.timestamp).toISOString()}`);
//             console.log(`Device Position (X, Y, Z): (${data.devicePositionX.toFixed(4)}, ${data.devicePositionY.toFixed(4)}, ${data.devicePositionZ.toFixed(4)})`);
//             console.log(`Device Rotation (X, Y, Z, W): (${data.deviceRotationX.toFixed(4)}, ${data.deviceRotationY.toFixed(4)}, ${data.deviceRotationZ.toFixed(4)}, ${data.deviceRotationW.toFixed(4)})`);
//             if (data.selectedNodeType) {
//                 console.log(`Selected Node Type: ${data.selectedNodeType}`);
//             }
//             console.log(`------------------------`);

//              wss.clients.forEach(client => {
//             if (client !== ws && client.readyState === WebSocket.OPEN) { // Don't send back to sender, ensure client is open
//                 client.send(JSON.stringify(data)); // Re-serialize the data and send
//             }
//         });

//             // Optional: You could also write this data to a file or a database here.
//             // Example for writing to a file (uncomment and ensure 'fs' is imported if used):
//             // const fs = require('fs');
//             // fs.appendFileSync('ar_pose_log.txt', `${JSON.stringify(data)}\n`);

//         } catch (error) {
//             // Handle cases where the message is not valid JSON
//             console.error(`\n🚨 Error parsing message: ${error.message}`);
//             console.error(`Received non-JSON message: ${message}`);
//         }
//     });

//     /**
//      * Event listener for when a client closes its connection.
//      */
//     ws.on('close', (code, reason) => {
//         console.log(`\n👋 Client disconnected. Code: ${code}, Reason: ${reason || 'N/A'}`);
//         console.log(`Total connected clients: ${wss.clients.size}`);
//     });

//     /**
//      * Event listener for WebSocket errors.
//      */
//     ws.on('error', error => {
//         console.error(`\n🔥 WebSocket error: ${error.message}`);
//     });
// });

// // Event listener for server-wide errors (e.g., port already in use)
// wss.on('error', error => {
//     console.error(`\n❌ Server error: ${error.message}`);
//     if (error.code === 'EADDRINUSE') {
//         console.error(`Port ${PORT} is already in use. Please choose a different port or stop the other process.`);
//     }
// });



// Import the WebSocket library
const WebSocket = require('ws');

// Define the port for the WebSocket server
// This port must be accessible from your Android device/emulator.
// For emulator, use 10.0.2.2:PORT. For physical device, use your machine's local IP:PORT.
const PORT = 8080;

// Create a new WebSocket server instance
const wss = new WebSocket.Server({ port: PORT });

console.log(`🚀 WebSocket server started on ws://localhost:${PORT}`);
console.log('Waiting for AR client connections...');

// Event listener for new WebSocket connections
wss.on('connection', ws => {
    console.log('\n✨ Client connected!');
    console.log(`Total connected clients: ${wss.clients.size}`);

    /**
     * Event listener for messages received from a client.
     * The AR client will send JSON strings containing pose data and node anchor data.
     */
    ws.on('message', message => {
        try {
            // Attempt to parse the incoming message as a JSON object
            const data = JSON.parse(message);

            // Handle different types of data based on the 'type' field
            switch (data.type) {
                case 'devicePose':
                    handleDevicePoseData(data);
                    break;
                
                case 'nodeAnchor':
                    handleNodeAnchorData(data);
                    break;
                
                case 'anchorUpdate':
                    handleAnchorUpdateData(data);
                    break;
                
                default:
                    console.log(`\n⚠️  Unknown data type: ${data.type}`);
                    console.log(`Raw data:`, data);
                    break;
            }

            // Broadcast the data to all other connected clients
            wss.clients.forEach(client => {
                if (client !== ws && client.readyState === WebSocket.OPEN) {
                    client.send(JSON.stringify(data));
                }
            });

        } catch (error) {
            // Handle cases where the message is not valid JSON
            console.error(`\n🚨 Error parsing message: ${error.message}`);
            console.error(`Received non-JSON message: ${message}`);
        }
    });

    /**
     * Event listener for when a client closes its connection.
     */
    ws.on('close', (code, reason) => {
        console.log(`\n👋 Client disconnected. Code: ${code}, Reason: ${reason || 'N/A'}`);
        console.log(`Total connected clients: ${wss.clients.size}`);
    });

    /**
     * Event listener for WebSocket errors.
     */
    ws.on('error', error => {
        console.error(`\n🔥 WebSocket error: ${error.message}`);
    });
});

/**
 * Handles device pose data (camera position and rotation)
 */
function handleDevicePoseData(data) {
    // console.log(`\n📱 === DEVICE POSE DATA ===`);
    // console.log(`Timestamp: ${new Date(data.timestamp).toISOString()}`);
    // console.log(`Device Position (X, Y, Z): (${data.devicePositionX.toFixed(4)}, ${data.devicePositionY.toFixed(4)}, ${data.devicePositionZ.toFixed(4)})`);
    // console.log(`Device Rotation (X, Y, Z, W): (${data.deviceRotationX.toFixed(4)}, ${data.deviceRotationY.toFixed(4)}, ${data.deviceRotationZ.toFixed(4)}, ${data.deviceRotationW.toFixed(4)})`);
    // if (data.selectedNodeType) {
    //     console.log(`Selected Node Type: ${data.selectedNodeType}`);
    // }
    // console.log(`============================`);
}

/**
 * Handles node anchor data (when new AR objects are created)
 */
function handleNodeAnchorData(data) {
    console.log(`\n🎯 === NEW AR NODE CREATED ===`);
    console.log(`Timestamp: ${new Date(data.timestamp).toISOString()}`);
    console.log(`Node ID: ${data.nodeId}`);
    console.log(`Node Type: ${data.nodeType}`);
    
    console.log(`\n📍 ANCHOR INFORMATION:`);
    console.log(`  Position (X, Y, Z): (${data.anchorPositionX.toFixed(4)}, ${data.anchorPositionY.toFixed(4)}, ${data.anchorPositionZ.toFixed(4)})`);
    console.log(`  Rotation (X, Y, Z, W): (${data.anchorRotationX.toFixed(4)}, ${data.anchorRotationY.toFixed(4)}, ${data.anchorRotationZ.toFixed(4)}, ${data.anchorRotationW.toFixed(4)})`);
    
    console.log(`\n🎲 NODE TRANSFORMATION:`);
    console.log(`  Position (X, Y, Z): (${data.nodePositionX.toFixed(4)}, ${data.nodePositionY.toFixed(4)}, ${data.nodePositionZ.toFixed(4)})`);
    console.log(`  Rotation (X, Y, Z, W): (${data.nodeRotationX.toFixed(4)}, ${data.nodeRotationY.toFixed(4)}, ${data.nodeRotationZ.toFixed(4)}, ${data.nodeRotationW.toFixed(4)})`);
    console.log(`  Scale (X, Y, Z): (${data.nodeScaleX.toFixed(4)}, ${data.nodeScaleY.toFixed(4)}, ${data.nodeScaleZ.toFixed(4)})`);
    
    // Handle cloud anchor specific data
    if (data.cloudAnchorId) {
        console.log(`\n☁️  CLOUD ANCHOR:`);
        console.log(`  Cloud ID: ${data.cloudAnchorId}`);
        console.log(`  State: ${data.cloudAnchorState}`);
    }
    
    console.log(`===============================`);
    
    // You can store this data for later use to replace objects
    // Example: Store in memory, database, or file
    storeNodeData(data);
}

/**
 * Handles legacy anchor update data
 */
function handleAnchorUpdateData(data) {
    console.log(`\n🔄 === ANCHOR UPDATE ===`);
    console.log(`Timestamp: ${new Date(data.timestamp).toISOString()}`);
    console.log(`Anchor ID: ${data.anchorId}`);
    console.log(`Anchor Type: ${data.anchorType}`);
    console.log(`Position (X, Y, Z): (${data.anchorPositionX.toFixed(4)}, ${data.anchorPositionY.toFixed(4)}, ${data.anchorPositionZ.toFixed(4)})`);
    console.log(`Rotation (X, Y, Z, W): (${data.anchorRotationX.toFixed(4)}, ${data.anchorRotationY.toFixed(4)}, ${data.anchorRotationZ.toFixed(4)}, ${data.anchorRotationW.toFixed(4)})`);
    if (data.anchorColor) {
        console.log(`Color: ${data.anchorColor}`);
    }
    console.log(`========================`);
}

/**
 * Store node data for future use (replace with your preferred storage method)
 */
const storedNodes = new Map();

function storeNodeData(data) {
    storedNodes.set(data.nodeId, {
        nodeId: data.nodeId,
        nodeType: data.nodeType,
        timestamp: data.timestamp,
        anchor: {
            position: { x: data.anchorPositionX, y: data.anchorPositionY, z: data.anchorPositionZ },
            rotation: { x: data.anchorRotationX, y: data.anchorRotationY, z: data.anchorRotationZ, w: data.anchorRotationW }
        },
        node: {
            position: { x: data.nodePositionX, y: data.nodePositionY, z: data.nodePositionZ },
            rotation: { x: data.nodeRotationX, y: data.nodeRotationY, z: data.nodeRotationZ, w: data.nodeRotationW },
            scale: { x: data.nodeScaleX, y: data.nodeScaleY, z: data.nodeScaleZ }
        },
        cloudAnchor: data.cloudAnchorId ? {
            id: data.cloudAnchorId,
            state: data.cloudAnchorState
        } : null
    });
    
    console.log(`💾 Stored node data. Total stored nodes: ${storedNodes.size}`);
    
    // Optional: Log all stored nodes
    if (storedNodes.size <= 10) { // Only show if not too many
        console.log(`📋 Current stored nodes: ${Array.from(storedNodes.keys()).join(', ')}`);
    }
}

/**
 * Helper function to get all stored nodes (for future use)
 */
function getAllStoredNodes() {
    return Array.from(storedNodes.values());
}

/**
 * Helper function to get a specific node by ID (for future use)
 */
function getNodeById(nodeId) {
    return storedNodes.get(nodeId);
}

// Event listener for server-wide errors (e.g., port already in use)
wss.on('error', error => {
    console.error(`\n❌ Server error: ${error.message}`);
    if (error.code === 'EADDRINUSE') {
        console.error(`Port ${PORT} is already in use. Please choose a different port or stop the other process.`);
    }
});

// Optional: Log stored nodes every 30 seconds for monitoring
setInterval(() => {
    if (storedNodes.size > 0) {
        console.log(`\n📊 Status: ${storedNodes.size} nodes stored, ${wss.clients.size} clients connected`);
    }
}, 30000);