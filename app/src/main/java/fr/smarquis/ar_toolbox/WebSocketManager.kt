package fr.smarquis.ar_toolbox

import android.content.Context
import android.widget.Toast
import com.google.ar.core.Pose
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages WebSocket communication for sending AR pose data and other information to a server.
 *
 * @param context The application context, used for Toast messages.
 * @param serverUrl The URL of the WebSocket server (e.g., "ws://10.0.2.2:8080" for emulator,
 * or "ws://YOUR_COMPUTER_LOCAL_IP:8080" for physical device).
 */
class WebSocketManager(private val context: Context, private val serverUrl: String) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable read timeout for continuous streaming
        .build()

    /**
     * Initializes and opens the WebSocket connection.
     * This should be called when the activity starts or is ready to communicate.
     */
    fun initWebSocket() {
        val request = Request.Builder().url(serverUrl).build()

        // Asynchronously create the WebSocket connection
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                println("WebSocket opened: ${response.message}")
                // Show a Toast on the UI thread as Toast messages must be on the main thread
                (context as? ArActivity<*>)?.runOnUiThread {
                    Toast.makeText(context, "Connected to server!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println("Received from server: $text")
                // TODO: Future enhancement: Implement logic to handle messages from the server
                // (e.g., commands to change anchor visibility, update asset properties)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                println("WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                println("WebSocket closed: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("WebSocket failure: ${t.message}")
                // Show a Toast on the UI thread for connection errors
                (context as? ArActivity<*>)?.runOnUiThread {
                    Toast.makeText(context, "Connection error: ${t.message}", Toast.LENGTH_LONG).show()
                }
                // TODO: Future enhancement: Implement retry logic here if the connection is critical
            }
        })
    }

    /**
     * Sends the current AR camera pose and selected node type to the WebSocket server.
     * Data is serialized into a JSON string.
     *
     * @param pose The ARCore Pose object containing translation (position) and rotation (quaternion).
     * @param selectedNodeType The simple name of the currently selected AR node type (e.g., "Sphere", "Cube").
     */
    fun sendPoseData(pose: Pose, selectedNodeType: String?) {
        // Only send data if the WebSocket connection is open
        if (webSocket == null) {
            println("WebSocket is not initialized or closed. Cannot send device pose data.")
            return
        }

        val translation = pose.translation // float array [x, y, z]
        val rotation = pose.rotationQuaternion // float array [x, y, z, w]

        // Create a JSON object to send with type "devicePose"
        val dataToSend = JSONObject().apply {
            put("type", "devicePose") // Indicate the type of data
            put("timestamp", System.currentTimeMillis()) // Current time in milliseconds
            put("devicePositionX", translation[0])
            put("devicePositionY", translation[1])
            put("devicePositionZ", translation[2])
            put("deviceRotationX", rotation[0])
            put("deviceRotationY", rotation[1])
            put("deviceRotationZ", rotation[2])
            put("deviceRotationW", rotation[3])
            // Include the selected node type if available
            selectedNodeType?.let { put("selectedNodeType", it) }
        }

        // Send the JSON data as a string over the WebSocket
        webSocket?.send(dataToSend.toString())
    }

    /**
     * Sends anchor and transformation data for a newly created AR node to the WebSocket server.
     * This includes the anchor position and the node's world transformation needed to place a replacement object.
     *
     * @param node The AR node that was just created
     */
    fun sendNodeAnchorData(node: Nodes) {
        if (webSocket == null) {
            println("WebSocket is not initialized or closed. Cannot send node anchor data.")
            return
        }

        val anchor = node.anchor()
        if (anchor == null) {
            println("Node has no anchor, cannot send anchor data.")
            return
        }

        val translation = anchor.pose.translation
        val rotation = anchor.pose.rotationQuaternion

        val dataToSend = JSONObject().apply {
            put("type", "nodeAnchor") // Indicate this is anchor data for a new node
            put("timestamp", System.currentTimeMillis())
            
            // Essential node identification
            put("nodeId", node.name) // Unique identifier: e.g., "Sphere #1", "Cube #2"
            put("nodeType", node::class.simpleName) // Node type: "Sphere", "Cube", "Drawing", etc.
            
            // Core anchor position and rotation - the base position where the object is anchored
            put("anchorPositionX", translation[0])
            put("anchorPositionY", translation[1])
            put("anchorPositionZ", translation[2])
            put("anchorRotationX", rotation[0])
            put("anchorRotationY", rotation[1])
            put("anchorRotationZ", rotation[2])
            put("anchorRotationW", rotation[3])
            
            // Node world transformation - the actual position, rotation, and scale of the object
            put("nodePositionX", node.worldPosition.x)
            put("nodePositionY", node.worldPosition.y)
            put("nodePositionZ", node.worldPosition.z)
            put("nodeRotationX", node.worldRotation.x)
            put("nodeRotationY", node.worldRotation.y)
            put("nodeRotationZ", node.worldRotation.z)
            put("nodeRotationW", node.worldRotation.w)
            put("nodeScaleX", node.worldScale.x)
            put("nodeScaleY", node.worldScale.y)
            put("nodeScaleZ", node.worldScale.z)
            
            // Include cloud anchor ID if this is a cloud anchor (for persistent placement across sessions)
            if (node is CloudAnchor) {
                node.id()?.let { put("cloudAnchorId", it) }
                put("cloudAnchorState", node.state()?.name ?: "UNKNOWN")
            }
        }

        webSocket?.send(dataToSend.toString())
        println("Sent anchor data: ${node.name} (${node::class.simpleName}) - Anchor: [${translation[0]}, ${translation[1]}, ${translation[2]}], Node: [${node.worldPosition.x}, ${node.worldPosition.y}, ${node.worldPosition.z}]")
    }


    /**
     * Sends data about a newly created or updated AR anchor to the WebSocket server.
     *
     * @param anchorId A unique identifier for the anchor.
     * @param anchorPose The ARCore Pose object of the anchor.
     * @param anchorType The type of AR node attached to this anchor (e.g., "Sphere", "Cube", "CloudAnchor").
     * @param anchorColor (Optional) The color of the anchor's material, in ARGB integer format.
     */
    fun sendAnchorData(anchorId: String, anchorPose: Pose, anchorType: String, anchorColor: Int? = null) {
        if (webSocket == null) {
            println("WebSocket is not initialized or closed. Cannot send anchor data.")
            return
        }

        val translation = anchorPose.translation
        val rotation = anchorPose.rotationQuaternion

        val dataToSend = JSONObject().apply {
            put("type", "anchorUpdate") // Indicate the type of data
            put("timestamp", System.currentTimeMillis())
            put("anchorId", anchorId)
            put("anchorType", anchorType)
            put("anchorPositionX", translation[0])
            put("anchorPositionY", translation[1])
            put("anchorPositionZ", translation[2])
            put("anchorRotationX", rotation[0])
            put("anchorRotationY", rotation[1])
            put("anchorRotationZ", rotation[2])
            put("anchorRotationW", rotation[3])
            anchorColor?.let { put("anchorColor", String.format("#%08X", it)) } // Convert ARGB int to hex string
        }

        webSocket?.send(dataToSend.toString())
    }

    /**
     * Closes the WebSocket connection gracefully.
     * This should be called when the activity is paused or destroyed.
     */
    fun closeWebSocket() {
        // Close with a normal closure code (1000) and a reason
        webSocket?.close(1000, "Client closing connection")
        webSocket = null // Clear the reference
        // Shut down OkHttpClient's internal thread pool to release resources
        client.dispatcher.executorService.shutdown()
    }
}
