package fr.smarquis.ar_toolbox

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Config.CloudAnchorMode
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Config.FocusMode
import com.google.ar.core.Config.LightEstimationMode
import com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
import com.google.ar.core.Config.UpdateMode
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingFailureReason.BAD_STATE
import com.google.ar.core.TrackingFailureReason.CAMERA_UNAVAILABLE
import com.google.ar.core.TrackingFailureReason.EXCESSIVE_MOTION
import com.google.ar.core.TrackingFailureReason.INSUFFICIENT_FEATURES
import com.google.ar.core.TrackingFailureReason.INSUFFICIENT_LIGHT
import com.google.ar.core.TrackingFailureReason.NONE
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingState.PAUSED
import com.google.ar.core.TrackingState.STOPPED
import com.google.ar.core.TrackingState.TRACKING
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.rendering.PlaneRenderer
import fr.smarquis.ar_toolbox.databinding.ActivitySceneBinding
import fr.smarquis.ar_toolbox.databinding.DialogInputBinding
// app/src/main/java/fr/smarquis/ar_toolbox/SceneActivity.kt

// ... existing imports ...
import fr.smarquis.ar_toolbox.WebSocketManager // Import the new WebSocketManager class
import com.google.ar.core.Pose // Ensure Pose is imported for type hinting
import org.json.JSONObject // If you plan to use JSONObject directly in SceneActivity for other purposes
import androidx.core.net.toUri // For converting strings to URI
// ...

/**
 * Data class to store exact node creation parameters for recreation
 */
data class NodeCreationParams(
    val nodeType: kotlin.reflect.KClass<out Nodes>,
    val materialProperties: MaterialProperties?,
    val externalUri: android.net.Uri?,
    val anchorData: AnchorData
)

/**
 * Data class to store anchor information for recreation
 */
data class AnchorData(
    val position: com.google.ar.sceneform.math.Vector3,
    val rotation: com.google.ar.sceneform.math.Quaternion
)

class SceneActivity : ArActivity<ActivitySceneBinding>(ActivitySceneBinding::inflate), ObjectMappingListener {

    private val coordinator by lazy { Coordinator(this, ::onArTap, ::onNodeSelected, ::onNodeFocused) }
    private val model: SceneViewModel by viewModels()
    private val settings by lazy { Settings.instance(this) }
    private var drawing: Drawing? = null

    lateinit var webSocketManager: WebSocketManager
    private val WEBSOCKET_SERVER_URL = "ws://192.168.1.4:8080" // Default for emulator
    
    // Mode tracking (anchor or viewer)
    private var appMode: String = "anchor" // default to anchor mode
    
    // Location and venue data
    private var venueName: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var accuracy: Float = 0.0f
    
    // Simple tracking for delete-previous logic
    var lastCreatedNode: Nodes? = null
    
    // Store creation parameters for recreation
    private var previousNodeParams: NodeCreationParams? = null


    private val setOfMaterialViews by lazy {
        with(bottomSheetNode.body) {
            setOf(
                colorValue,
                colorLabel,
                metallicValue,
                metallicLabel,
                roughnessValue,
                roughnessLabel,
                reflectanceValue,
                reflectanceLabel,
            )
        }
    }
    private val setOfCloudAnchorViews by lazy {
        with(bottomSheetNode.body) {
            setOf(
                cloudAnchorStateLabel,
                cloudAnchorStateValue,
                cloudAnchorIdLabel,
                cloudAnchorIdValue,
            )
        }
    }
    private val setOfMeasureViews by lazy {
        with(bottomSheetNode) {
            setOf(
                header.undo,
                body.measureLabel,
                body.measureValue,
            )
        }
    }

    override val arSceneView: ArSceneView get() = binding.arSceneView

    override val recordingIndicator: ImageView get() = bottomSheetScene.header.recording

    private val bottomSheetScene get() = binding.bottomSheetScene

    private val bottomSheetNode get() = binding.bottomSheetNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get the mode from intent
        appMode = intent.getStringExtra("mode") ?: "anchor"
        
        // Get location data from intent (if coming from LocationLoadingActivity)
        venueName = intent.getStringExtra("venue_name")
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        accuracy = intent.getFloatExtra("accuracy", 0.0f)
        
        initSceneBottomSheet()
        initNodeBottomSheet()
        initAr()
        initWithIntent(intent)
        webSocketManager = WebSocketManager(this, WEBSOCKET_SERVER_URL)
        // Note: Not using object mapping listener in simplified delete-previous mode
        webSocketManager.initWebSocket()
        
        // Configure UI based on mode
        configureUIForMode()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initWithIntent(intent)
    }
    
    private fun configureUIForMode() {
        when (appMode) {
            "anchor" -> {
                // Anchor mode: Show only essential elements
                supportActionBar?.title = "Expolens - Anchor Mode"
                Toast.makeText(this, "🔗 Anchor Mode: Create and place AR objects", Toast.LENGTH_LONG).show()
                
                // Show venue information if available
                if (venueName != null) {
                    binding.venueDisplay.visibility = View.VISIBLE
                    binding.venueName.text = venueName
                    binding.venueCoordinates.text = String.format("%.4f, %.4f", latitude, longitude)
                    
                    // Adjust top margin to account for status bar
                    val statusBarHeight = getStatusBarHeight()
                    val layoutParams = binding.venueDisplay.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
                    layoutParams.topMargin = statusBarHeight + 16 // 16dp additional margin
                    binding.venueDisplay.layoutParams = layoutParams
                    
                    // Send location data to server
                    sendLocationDataToServer()
                }
                
                // Show only: URL option (link), shapes (sphere, cube, cylinder), stats and color remain visible
                with(bottomSheetScene.body) {
                    // Keep these visible:
                    sphere.visibility = View.VISIBLE      // Shape
                    cylinder.visibility = View.VISIBLE    // Shape  
                    cube.visibility = View.VISIBLE        // Shape
                    link.visibility = View.VISIBLE        // URL option
                    colorValue.visibility = View.VISIBLE  // Color option
                    
                    // Hide everything else:
                    view.visibility = View.GONE           // Layout
                    drawing.visibility = View.GONE        // Drawing
                    measure.visibility = View.GONE        // Measure
                    andy.visibility = View.GONE           // Andy
                    video.visibility = View.GONE          // Video
                    cloudAnchor.visibility = View.GONE    // Cloud Anchor
                }
            }
            "viewer" -> {
                // Viewer mode: Identical functionality to Anchor mode (just a demonstration gimmick)
                supportActionBar?.title = "Expolens - Viewer Mode"
                Toast.makeText(this, "👁️ Viewer Mode: View and track AR objects", Toast.LENGTH_LONG).show()
                
                // Exact same UI as Anchor mode
                with(bottomSheetScene.body) {
                    // Keep these visible:
                    sphere.visibility = View.VISIBLE      // Shape
                    cylinder.visibility = View.VISIBLE    // Shape  
                    cube.visibility = View.VISIBLE        // Shape
                    link.visibility = View.VISIBLE        // URL option
                    colorValue.visibility = View.VISIBLE  // Color option
                    
                    // Hide everything else:
                    view.visibility = View.GONE           // Layout
                    drawing.visibility = View.GONE        // Drawing
                    measure.visibility = View.GONE        // Measure
                    andy.visibility = View.GONE           // Andy
                    video.visibility = View.GONE          // Video
                    cloudAnchor.visibility = View.GONE    // Cloud Anchor
                }
            }
        }
    }
    
    private fun sendLocationDataToServer() {
        if (venueName != null) {
            webSocketManager.sendLocationData(venueName!!, latitude, longitude, accuracy)
        }
    }
    
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            // Fallback: approximate status bar height in dp converted to pixels
            val density = resources.displayMetrics.density
            (24 * density).toInt() // 24dp is typical status bar height
        }
    }

    override fun onBackPressed() {
        if (coordinator.selectedNode != null) {
            coordinator.selectNode(null)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        // --- NEW: Close WebSocket connection when the activity pauses ---
        webSocketManager.closeWebSocket()
        // --- END NEW ---
    }

    override fun onDestroy() {
        super.onDestroy()
        // --- NEW: Ensure WebSocket connection is closed when the activity is destroyed ---
        webSocketManager.closeWebSocket()
        // --- END NEW ---
    }

    override fun config(session: Session): Config = Config(session).apply {
        lightEstimationMode = LightEstimationMode.DISABLED
        planeFindingMode = HORIZONTAL_AND_VERTICAL
        updateMode = UpdateMode.LATEST_CAMERA_IMAGE
        cloudAnchorMode = CloudAnchorMode.ENABLED
        augmentedImageDatabase = AugmentedImageDatabase(session).apply {
            Augmented.target(this@SceneActivity)
                ?.runCatching { addImage("augmented", this) }
                ?.onFailure(Throwable::printStackTrace) // Might throw ImageInsufficientQualityException
        }
        augmentedFaceMode = AugmentedFaceMode.DISABLED
        focusMode = FocusMode.AUTO
        if (session.isDepthModeSupported(DepthMode.AUTOMATIC)) {
            depthMode = DepthMode.AUTOMATIC
        }
    }

    override fun onArResumed() {
        bottomSheetScene.behavior().update(state = STATE_EXPANDED, isHideable = false)
        bottomSheetScene.body.cameraValue.text = arSceneView.session?.cameraConfig?.format(this)
    }

    private fun initWithIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        intent.data?.let {
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
            selectExternalModel(it.toString())
            this.intent = null
        }
    }

    private fun initSceneBottomSheet() = with(bottomSheetScene) {
        behavior().apply {
            state = STATE_HIDDEN
            configureBottomSheetAnimatedForegroundMask(body)
            configureBottomSheetInset(inset)
        }
        header.root.setOnClickListener { behavior().toggle() }
        header.add.setOnClickListener {
            val session = arSceneView.session
            val camera = arSceneView.arFrame?.camera ?: return@setOnClickListener
            if (session == null || camera.trackingState != TRACKING) return@setOnClickListener
            createNodeAndAddToScene(anchor = { session.createAnchor(Nodes.defaultPose(arSceneView)) }, focus = false)
        }

        initPopupMenu(
            anchor = header.more,
            menu = R.menu.menu_scene,
            onClick = {
                when (it.itemId) {
                    R.id.menu_item_resolve_cloud_anchor -> promptCloudAnchorId()
                    R.id.menu_item_clean_up_scene -> arSceneView.scene.callOnHierarchy { node -> (node as? Nodes)?.detach() }
                    R.id.menu_item_sunlight -> settings.sunlight.toggle(it, arSceneView)
                    R.id.menu_item_shadows -> settings.shadows.toggle(it, arSceneView)
                    R.id.menu_item_plane_renderer -> settings.planes.toggle(it, arSceneView)
                    R.id.menu_item_selection_visualizer -> settings.selection.toggle(it, coordinator.selectionVisualizer)
                    R.id.menu_item_reticle -> settings.reticle.toggle(it, arSceneView)
                    R.id.menu_item_point_cloud -> settings.pointCloud.toggle(it, arSceneView)
                }
                when (it.itemId) {
                    R.id.menu_item_sunlight, R.id.menu_item_shadows, R.id.menu_item_plane_renderer, R.id.menu_item_selection_visualizer, R.id.menu_item_reticle, R.id.menu_item_point_cloud -> false
                    else -> true
                }
            },
            onUpdate = {
                findItem(R.id.menu_item_clean_up_scene).isEnabled = arSceneView.scene.findInHierarchy { it is Nodes } != null
                settings.apply {
                    sunlight.applyTo(findItem(R.id.menu_item_sunlight))
                    shadows.applyTo(findItem(R.id.menu_item_shadows))
                    planes.applyTo(findItem(R.id.menu_item_plane_renderer))
                    selection.applyTo(findItem(R.id.menu_item_selection_visualizer))
                    reticle.applyTo(findItem(R.id.menu_item_reticle))
                    pointCloud.applyTo(findItem(R.id.menu_item_point_cloud))
                }
            },
        )

        model.selection.observe(this@SceneActivity) {
            body.apply {
                sphere.isSelected = it == Sphere::class
                cylinder.isSelected = it == Cylinder::class
                cube.isSelected = it == Cube::class
                measure.isSelected = it == Measure::class
                view.isSelected = it == Layout::class
                andy.isSelected = it == Andy::class
                video.isSelected = it == Video::class
                drawing.isSelected = it == Drawing::class
                link.isSelected = it == Link::class
                cloudAnchor.isSelected = it == CloudAnchor::class
            }
            header.add.requestDisallowInterceptTouchEvent = it == Drawing::class
        }

        body.apply {
            sphere.setOnClickListener { model.selection.value = Sphere::class }
            cylinder.setOnClickListener { model.selection.value = Cylinder::class }
            cube.setOnClickListener { model.selection.value = Cube::class }
            view.setOnClickListener { model.selection.value = Layout::class }
            drawing.setOnClickListener { model.selection.value = Drawing::class }
            measure.setOnClickListener { model.selection.value = Measure::class }
            andy.setOnClickListener { model.selection.value = Andy::class }
            video.setOnClickListener { model.selection.value = Video::class }
            link.setOnClickListener { promptExternalModel() }
            cloudAnchor.setOnClickListener { model.selection.value = CloudAnchor::class }
            colorValue.setOnColorChangeListener { color ->
                arSceneView.planeRenderer.material?.thenAccept {
                    it.setFloat3(PlaneRenderer.MATERIAL_COLOR, color.toArColor())
                }
                settings.pointCloud.updateMaterial(arSceneView) { this.color = color }
                settings.reticle.updateMaterial(arSceneView) { this.color = color }
            }
            colorValue.post { colorValue.setColor(MaterialProperties.DEFAULT.color) }
        }
    }

    private fun initNodeBottomSheet() = with(bottomSheetNode) {
        behavior().apply {
            skipCollapsed = true
            addBottomSheetCallback(object : BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    bottomSheet.requestLayout()
                    if (newState == STATE_HIDDEN) {
                        coordinator.selectNode(null)
                    }
                }
            })
            state = STATE_HIDDEN
            configureBottomSheetInset(inset)
        }
        header.apply {
            root.setOnClickListener { coordinator.selectNode(null) }
            copy.setOnClickListener { (coordinator.focusedNode as? CloudAnchor)?.copyToClipboard(this@SceneActivity) }
            playPause.setOnClickListener { (coordinator.focusedNode as? Video)?.toggle() }
            delete.setOnClickListener { coordinator.focusedNode?.detach() }
            undo.setOnClickListener { (coordinator.focusedNode as? Measure)?.undo() }
        }

        body.apply {
            colorValue.setOnColorChangeListener { focusedMaterialNode()?.update { color = it } }
            metallicValue.progress = MaterialProperties.DEFAULT.metallic
            metallicValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener { focusedMaterialNode()?.update { metallic = it } })
            roughnessValue.progress = MaterialProperties.DEFAULT.roughness
            roughnessValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener { focusedMaterialNode()?.update { roughness = it } })
            reflectanceValue.progress = MaterialProperties.DEFAULT.reflectance
            reflectanceValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener { focusedMaterialNode()?.update { reflectance = it } })
        }
    }

    private fun focusedMaterialNode() = (coordinator.focusedNode as? MaterialNode)

    private fun materialProperties() = with(bottomSheetNode.body) {
        MaterialProperties(
            color = if (focusedMaterialNode() != null) colorValue.getColor() else bottomSheetScene.body.colorValue.getColor(),
            metallic = metallicValue.progress,
            roughness = roughnessValue.progress,
            reflectance = reflectanceValue.progress,
        )
    }

    private fun initAr() = with(arSceneView) {
        scene.addOnUpdateListener { onArUpdate() }
        scene.addOnPeekTouchListener { hitTestResult, motionEvent ->
            coordinator.onTouch(hitTestResult, motionEvent)
            if (shouldHandleDrawing(motionEvent, hitTestResult)) {
                val x = motionEvent.x
                val y = motionEvent.y
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> drawing = Drawing.create(x, y, true, materialProperties(), this, coordinator, settings)
                    MotionEvent.ACTION_MOVE -> drawing?.extend(x, y)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> drawing = drawing?.deleteIfEmpty().let { null }
                }
            }
        }
        settings.apply {
            sunlight.applyTo(this@with)
            shadows.applyTo(this@with)
            planes.applyTo(this@with)
            selection.applyTo(coordinator.selectionVisualizer)
            reticle.initAndApplyTo(this@with)
            pointCloud.initAndApplyTo(this@with)
        }
    }

    private fun shouldHandleDrawing(motionEvent: MotionEvent? = null, hitTestResult: HitTestResult? = null): Boolean {
        if (model.selection.value != Drawing::class) return false
        if (coordinator.selectedNode?.isTransforming == true) return false
        if (arSceneView.arFrame?.camera?.trackingState != TRACKING) return false
        if (motionEvent?.action == MotionEvent.ACTION_DOWN && hitTestResult?.node != null) return false
        return true
    }

    private fun promptExternalModel() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
            .setItems(R.array.models_labels) { _, i ->
                if (i == 0) {
                    promptExternalModelUri()
                } else {
                    selectExternalModel(resources.getStringArray(R.array.models_uris)[i])
                }
            }
            .create()
            .show()
    }

    private fun prompt(block: DialogInputBinding.(AlertDialog.Builder) -> Unit) = DialogInputBinding.inflate(LayoutInflater.from(ContextThemeWrapper(this, R.style.AlertDialog)), null, false).apply {
        block(AlertDialog.Builder(root.context).setView(root))
    }

    private fun promptExternalModelUri() = prompt { builder ->
        layout.hint = getText(R.string.model_link_custom_hint)
        value.inputType = InputType.TYPE_TEXT_VARIATION_URI
        value.setText(model.externalModelUri.value.takeUnless { it in resources.getStringArray(R.array.models_uris) })
        builder.setPositiveButton(android.R.string.ok) { _, _ -> selectExternalModel(value.text.toString()) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setCancelable(false)
            .show()
        value.requestFocus()
    }

    private fun selectExternalModel(value: String) {
        model.externalModelUri.value = value
        model.selection.value = Link::class
        Link.warmup(this, value.toUri())
    }

    private fun promptCloudAnchorId() = prompt { builder ->
        layout.hint = getText(R.string.cloud_anchor_id_hint)
        value.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val dialog = builder.setPositiveButton(R.string.cloud_anchor_id_resolve) { _, _ ->
            CloudAnchor.resolve(value.text.toString(), applicationContext, arSceneView, coordinator, settings)?.also {
                coordinator.focusNode(it)
            }
        }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setCancelable(false)
            .show()
        value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = !s.isNullOrBlank()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        value.text = value.text
        value.requestFocus()
    }

    private fun onArTap(motionEvent: MotionEvent) {
        val frame = arSceneView.arFrame ?: return
        if (frame.camera.trackingState != TRACKING) {
            coordinator.selectNode(null)
            return
        }

        // Check if we're placing a Link node (URL-based model)
        val isLinkNode = model.selection.value == Link::class
        
        if (isLinkNode) {
            // For Link nodes, use mid-air placement like doodles
            val session = arSceneView.session ?: return
            val ray = arSceneView.scene.camera.screenPointToRay(motionEvent.x, motionEvent.y)
            val point = ray.getPoint(0.5f) // 0.5 meters in front of camera
            val midAirPose = com.google.ar.core.Pose.makeTranslation(point.x, point.y, point.z)
            
            createNodeAndAddToScene(anchor = { session.createAnchor(midAirPose) })
            println("🎯 Link node placed in mid-air at 0.5m from camera")
        } else {
            // For other nodes, use normal plane-based placement
            frame.hitTest(motionEvent).firstOrNull {
                val trackable = it.trackable
                when {
                    trackable is Plane && trackable.isPoseInPolygon(it.hitPose) -> true
                    trackable is DepthPoint -> true
                    trackable is Point -> true
                    else -> false
                }
            }?.let { createNodeAndAddToScene(anchor = { it.createAnchor() }) } ?: coordinator.selectNode(null)
        }
    }

    private fun createNodeAndAddToScene(anchor: () -> Anchor, focus: Boolean = true) {
        // Step 1: Delete previous node if it exists - COMMENTED OUT FOR SMOOTH PLACEMENT
        /*
        lastCreatedNode?.let { previousNode ->
            println("🗑️ Deleting previous node: ${previousNode.name}")
            previousNode.setParent(null)
            lastCreatedNode = null
        }
        */
        
        // Step 2: Create the new node with exact parameter storage
        val currentAnchor = anchor()
        val anchorPose = currentAnchor.pose
        val anchorData = AnchorData(
            position = com.google.ar.sceneform.math.Vector3(anchorPose.translation[0], anchorPose.translation[1], anchorPose.translation[2]),
            rotation = com.google.ar.sceneform.math.Quaternion(anchorPose.rotationQuaternion[0], anchorPose.rotationQuaternion[1], anchorPose.rotationQuaternion[2], anchorPose.rotationQuaternion[3])
        )
        
        val currentMaterials = when (model.selection.value) {
            Sphere::class, Cylinder::class, Cube::class, Measure::class -> materialProperties()
            else -> null
        }
        
        val currentUri = when (model.selection.value) {
            Link::class -> model.externalModelUri.value.orEmpty().toUri()
            else -> null
        }
        
        val node = when (model.selection.value) {
            Sphere::class -> Sphere(this, currentMaterials!!, coordinator, settings)
            Cylinder::class -> Cylinder(this, currentMaterials!!, coordinator, settings)
            Cube::class -> Cube(this, currentMaterials!!, coordinator, settings)
            Layout::class -> Layout(this, coordinator, settings)
            Andy::class -> Andy(this, coordinator, settings)
            Video::class -> Video(this, coordinator, settings)
            Measure::class -> Measure(this, currentMaterials!!, coordinator, settings)
            Link::class -> Link(this, currentUri!!, coordinator, settings)
            CloudAnchor::class -> CloudAnchor(this, arSceneView.session ?: return, coordinator, settings)
            else -> return
        }
        
        // Step 3: Attach the new node
        node.attach(currentAnchor, arSceneView.scene, focus)
        // lastCreatedNode = node  // COMMENTED OUT - No longer tracking for deletion
        println("✅ Created new node: ${node.name}")
        
        // Step 4: Store current node parameters for future recreation
        val currentNodeParams = NodeCreationParams(
            nodeType = model.selection.value!!,
            materialProperties = currentMaterials,
            externalUri = currentUri,
            anchorData = anchorData
        )
        
        // Step 5: If we have previous parameters, schedule recreation after 2 seconds - COMMENTED OUT FOR SMOOTH PLACEMENT
        /*
        previousNodeParams?.let { prevParams ->
            println("⏰ Scheduling recreation of previous node in 2 seconds...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                recreateNodeFromParams(prevParams)
            }, 2000) // 2 seconds delay
        }
        
        // Step 6: Store current parameters as previous for next time
        previousNodeParams = currentNodeParams
        */
        
        // Send anchor data to server for 3D mapping (logging and tracking still intact)
        webSocketManager.sendNodeAnchorData(node)
    }
    
    /**
     * Shows a dialog for Link nodes and schedules auto-replacement after 10 seconds - COMMENTED OUT FOR STATIC PLACEMENT
     */
    /*
    fun showLinkNodeDialog(linkNode: Link) {
        runOnUiThread {
            // Show dialog with model info
            val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
                .setTitle("🔗 Link Node Created")
                .setMessage("URL: ${linkNode.uri}\n\nThis node will be replaced with campsite model in 10 seconds...")
                .setPositiveButton("OK") { dialog, _ -> 
                    dialog.dismiss()
                }
                .setCancelable(true)
                .create()
            
            dialog.show()
            
            // Auto-dismiss dialog after 10 seconds and replace node
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                replaceWithCampsite(linkNode)
            }, 10000) // 10 seconds
            
            println("📋 Dialog shown for Link node: ${linkNode.name} with URL: ${linkNode.uri}")
        }
    }
    */
    
    /**
     * Replaces a Link node with the campsite model at the same location - COMMENTED OUT FOR STATIC PLACEMENT
     */
    /*
    private fun replaceWithCampsite(originalNode: Link) {
        println("🏕️ Replacing Link node with campsite model...")
        
        // Get the original node's anchor and position
        val originalAnchor = originalNode.anchor()
        if (originalAnchor == null) {
            println("❌ Cannot replace node - no anchor found")
            return
        }
        
        // Store the original position data
        val originalPose = originalAnchor.pose
        val anchorData = AnchorData(
            position = com.google.ar.sceneform.math.Vector3(
                originalPose.translation[0], 
                originalPose.translation[1], 
                originalPose.translation[2]
            ),
            rotation = com.google.ar.sceneform.math.Quaternion(
                originalPose.rotationQuaternion[0], 
                originalPose.rotationQuaternion[1], 
                originalPose.rotationQuaternion[2], 
                originalPose.rotationQuaternion[3]
            )
        )
        
        // Delete the original node
        originalNode.setParent(null)
        println("🗑️ Deleted original Link node: ${originalNode.name}")
        
        // Create new anchor at the same position
        val session = arSceneView.session ?: return
        val newPose = com.google.ar.core.Pose(
            floatArrayOf(anchorData.position.x, anchorData.position.y, anchorData.position.z),
            floatArrayOf(anchorData.rotation.x, anchorData.rotation.y, anchorData.rotation.z, anchorData.rotation.w)
        )
        val newAnchor = session.createAnchor(newPose)
        
        // Create new Link node with campsite URL
        val campsiteUri = "http://192.168.1.4:3000/models/camp_site.glb".toUri()
        val campsiteNode = Link(this, campsiteUri, coordinator, settings)
        
        // Attach to scene at exact same location
        campsiteNode.attach(newAnchor, arSceneView.scene, false)
        
        println("✅ Successfully replaced with campsite model at same location")
        
        // Send data to server for new campsite node
        webSocketManager.sendNodeAnchorData(campsiteNode)
        
        // Show confirmation dialog
        runOnUiThread {
            AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
                .setTitle("🏕️ Replaced with Campsite")
                .setMessage("The original model has been replaced with the campsite model at the same location.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
    */
    
    /**
     * Recreates a node from stored parameters at the exact same location
     */
    private fun recreateNodeFromParams(params: NodeCreationParams) {
        println("🔄 Recreating node: ${params.nodeType.simpleName}")
        
        // Create anchor at stored position and rotation
        val session = arSceneView.session ?: return
        val pose = com.google.ar.core.Pose(
            floatArrayOf(params.anchorData.position.x, params.anchorData.position.y, params.anchorData.position.z),
            floatArrayOf(params.anchorData.rotation.x, params.anchorData.rotation.y, params.anchorData.rotation.z, params.anchorData.rotation.w)
        )
        val recreatedAnchor = session.createAnchor(pose)
        
        // Determine final node type (with random shape selection for basic shapes)
        val finalNodeType = getRandomizedNodeType(params.nodeType)
        
        // Create node with potentially randomized shape/model but same other parameters
        val recreatedNode = when (finalNodeType) {
            Sphere::class -> Sphere(this, params.materialProperties!!, coordinator, settings)
            Cylinder::class -> Cylinder(this, params.materialProperties!!, coordinator, settings)
            Cube::class -> Cube(this, params.materialProperties!!, coordinator, settings)
            Layout::class -> Layout(this, coordinator, settings)
            Andy::class -> Andy(this, coordinator, settings)
            Video::class -> Video(this, coordinator, settings)
            Measure::class -> Measure(this, params.materialProperties!!, coordinator, settings)
            Link::class -> {
                // For Link nodes, replace with campsite model from localhost
                val campsiteUri = "http://192.168.1.4:3000/models/camp_fire.glb".toUri()
                println("🏕️ Recreating Link with campsite model: $campsiteUri")
                Link(this, campsiteUri, coordinator, settings)
            }
            CloudAnchor::class -> CloudAnchor(this, session, coordinator, settings)
            Drawing::class -> {
                // For Drawing, create a simple Drawing without extending (no line drawing)
                Drawing(false, null, params.materialProperties!!, coordinator, settings)
            }
            else -> return
        }
        
        // Attach to scene at exact stored location
        recreatedNode.attach(recreatedAnchor, arSceneView.scene, false) // no focus for recreated nodes
        
        val changeInfo = if (finalNodeType != params.nodeType) {
            " (changed from ${params.nodeType.simpleName} to ${finalNodeType.simpleName})"
        } else if (params.nodeType == Link::class) {
            " (replaced with campsite model)"
        } else {
            ""
        }
        
        println("✅ Successfully recreated: ${recreatedNode.name} at stored location$changeInfo")
        
        // Send data to server for recreated node
        webSocketManager.sendNodeAnchorData(recreatedNode)
    }
    
    /**
     * Randomly selects a shape for basic geometric objects (Sphere, Cube, Cylinder)
     * For Link nodes, replaces with campsite model for testing
     * Other node types are returned unchanged
     */
    private fun getRandomizedNodeType(originalType: kotlin.reflect.KClass<out Nodes>): kotlin.reflect.KClass<out Nodes> {
        return when (originalType) {
            Sphere::class, Cube::class, Cylinder::class -> {
                // Define the basic shapes
                val basicShapes = listOf(Sphere::class, Cube::class, Cylinder::class)
                
                // Remove the original type to ensure we always get a different shape
                val otherShapes = basicShapes.filter { it != originalType }
                
                // Randomly select from the other shapes
                val randomShape = otherShapes.random()
                
                println("🎲 Shape randomization: ${originalType.simpleName} → ${randomShape.simpleName}")
                randomShape
            }
            Link::class -> {
                // For Link nodes, we'll replace with campsite model
                println("🔗 Link randomization: Will replace with campsite model")
                Link::class
            }
            else -> {
                // Keep other node types unchanged (Layout, Andy, Video, etc.)
                println("🔄 Non-randomizable type: ${originalType.simpleName} - keeping unchanged")
                originalType
            }
        }
    }
    
    /**
     * Helper functions for Drawing parameter storage
     */
    fun createAnchorData(pose: com.google.ar.core.Pose): AnchorData {
        return AnchorData(
            position = com.google.ar.sceneform.math.Vector3(pose.translation[0], pose.translation[1], pose.translation[2]),
            rotation = com.google.ar.sceneform.math.Quaternion(pose.rotationQuaternion[0], pose.rotationQuaternion[1], pose.rotationQuaternion[2], pose.rotationQuaternion[3])
        )
    }
    
    fun createDrawingParams(properties: MaterialProperties, anchorData: AnchorData): NodeCreationParams {
        return NodeCreationParams(
            nodeType = Drawing::class,
            materialProperties = properties,
            externalUri = null,
            anchorData = anchorData
        )
    }
    
    fun createCloudAnchorParams(anchorData: AnchorData): NodeCreationParams {
        return NodeCreationParams(
            nodeType = CloudAnchor::class,
            materialProperties = null,
            externalUri = null,
            anchorData = anchorData
        )
    }
    
    fun schedulePreviousRecreation() {
        previousNodeParams?.let { prevParams ->
            println("⏰ Scheduling recreation of previous node in 2 seconds...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                recreateNodeFromParams(prevParams)
            }, 2000)
        }
    }
    
    fun storePreviousParams(params: NodeCreationParams) {
        previousNodeParams = params
    }

    private fun onArUpdate() {
        val frame = arSceneView.arFrame
        val camera = frame?.camera
        val state = camera?.trackingState
        val reason = camera?.trackingFailureReason

        onArUpdateStatusText(state, reason)
        onArUpdateStatusIcon(state, reason)
        onArUpdateBottomSheet(state)
        onArUpdateDrawing()
        onArUpdateAugmentedImages()

        val currentPose = camera?.pose
        // Only send data if the camera is tracking and pose is available
        if (currentPose != null && state == TrackingState.TRACKING) {
            // Get the simple name of the currently selected node type (e.g., "Sphere", "Cube")
            val selectedNodeType = model.selection.value?.simpleName
            webSocketManager.sendPoseData(currentPose, selectedNodeType)
        }
    }

    private fun onArUpdateStatusText(state: TrackingState?, reason: TrackingFailureReason?) = bottomSheetScene.header.label.setText(
        when (state) {
            TRACKING -> R.string.tracking_success
            PAUSED -> when (reason) {
                NONE -> R.string.tracking_failure_none
                BAD_STATE -> R.string.tracking_failure_bad_state
                INSUFFICIENT_LIGHT -> R.string.tracking_failure_insufficient_light
                EXCESSIVE_MOTION -> R.string.tracking_failure_excessive_motion
                INSUFFICIENT_FEATURES -> R.string.tracking_failure_insufficient_features
                CAMERA_UNAVAILABLE -> R.string.tracking_failure_camera_unavailable
                null -> 0
            }
            STOPPED -> R.string.tracking_stopped
            null -> 0
        },
    )

    private fun onArUpdateStatusIcon(state: TrackingState?, reason: TrackingFailureReason?) = bottomSheetScene.header.status.setImageResource(
        when (state) {
            TRACKING -> android.R.drawable.presence_online
            PAUSED -> when (reason) {
                NONE -> android.R.drawable.presence_invisible
                BAD_STATE, CAMERA_UNAVAILABLE -> android.R.drawable.presence_busy
                INSUFFICIENT_LIGHT, EXCESSIVE_MOTION, INSUFFICIENT_FEATURES -> android.R.drawable.presence_away
                null -> 0
            }
            STOPPED -> android.R.drawable.presence_offline
            null -> 0
        },
    )

    private fun onArUpdateBottomSheet(state: TrackingState?) = with(bottomSheetScene) {
        header.add.isEnabled = state == TRACKING
        when (behavior().state) {
            STATE_HIDDEN, STATE_COLLAPSED -> Unit
            else -> body.apply {
                arSceneView.arFrame?.camera?.pose.let {
                    poseTranslationValue.text = it.formatTranslation(this@SceneActivity)
                    poseRotationValue.text = it.formatRotation(this@SceneActivity)
                }
                sceneValue.text = arSceneView.session?.format(this@SceneActivity)
            }
        }
    }

    private fun onArUpdateDrawing() {
        if (shouldHandleDrawing()) {
            val x = arSceneView.width / 2F
            val y = arSceneView.height / 2F
            val pressed = bottomSheetScene.header.add.isPressed
            when {
                pressed && drawing == null -> drawing = Drawing.create(x, y, false, materialProperties(), arSceneView, coordinator, settings)
                pressed && drawing?.isFromTouch == false -> drawing?.extend(x, y)
                !pressed && drawing?.isFromTouch == false -> drawing = drawing?.deleteIfEmpty().let { null }
                else -> Unit
            }
        }
    }

    private fun onArUpdateAugmentedImages() {
        arSceneView.arFrame?.getUpdatedTrackables(AugmentedImage::class.java)?.forEach {
            Augmented.update(this, it, coordinator, settings)?.apply {
                attach(it.createAnchor(it.centerPose), arSceneView.scene)
                // Send anchor data to server for Augmented image nodes
                webSocketManager.sendNodeAnchorData(this)
            }
        }
    }

    private fun onNodeUpdate(node: Nodes) = with(bottomSheetNode) {
        when {
            node != coordinator.selectedNode || node != coordinator.focusedNode || behavior().state == STATE_HIDDEN -> Unit
            else -> {
                with(header) {
                    status.setImageResource(node.statusIcon())
                    distance.text = arSceneView.arFrame?.camera.formatDistance(this@SceneActivity, node)
                    copy.isEnabled = (node as? CloudAnchor)?.id() != null
                    playPause.isActivated = (node as? Video)?.isPlaying() == true
                    delete.isEnabled = !node.isTransforming
                }
                with(body) {
                    positionValue.text = node.worldPosition.format(this@SceneActivity)
                    rotationValue.text = node.worldRotation.format(this@SceneActivity)
                    scaleValue.text = node.worldScale.format(this@SceneActivity)
                    cloudAnchorStateValue.text = (node as? CloudAnchor)?.state()?.name
                    cloudAnchorIdValue.text = (node as? CloudAnchor)?.let { it.id() ?: "…" }
                    measureValue.text = (node as? Measure)?.formatMeasure()
                }
            }
        }
    }

    private fun onNodeSelected(old: Nodes? = coordinator.selectedNode, new: Nodes?) {
        old?.onNodeUpdate = null
        new?.onNodeUpdate = ::onNodeUpdate
    }

    private fun onNodeFocused(node: Nodes?) {
        val nodeSheetBehavior = bottomSheetNode.behavior()
        val sceneBehavior = bottomSheetScene.behavior()
        when (node) {
            null -> {
                nodeSheetBehavior.state = STATE_HIDDEN
                if ((bottomSheetScene.root.tag as? Boolean) == true) {
                    bottomSheetScene.root.tag = false
                    sceneBehavior.state = STATE_EXPANDED
                }
            }
            coordinator.selectedNode -> {
                with(bottomSheetNode.header) {
                    name.text = node.name
                    copy.visibility = if (node is CloudAnchor) VISIBLE else GONE
                    playPause.visibility = if (node is Video) VISIBLE else GONE
                }
                with(bottomSheetNode.body) {
                    (node as? MaterialNode)?.properties?.let {
                        colorValue.setColor(it.color)
                        metallicValue.progress = it.metallic
                        roughnessValue.progress = it.roughness
                        reflectanceValue.progress = it.reflectance
                    }
                }
                val materialVisibility = if (node is MaterialNode) VISIBLE else GONE
                setOfMaterialViews.forEach { it.visibility = materialVisibility }
                val cloudAnchorVisibility = if (node is CloudAnchor) VISIBLE else GONE
                setOfCloudAnchorViews.forEach { it.visibility = cloudAnchorVisibility }
                val measureVisibility = if (node is Measure) VISIBLE else GONE
                setOfMeasureViews.forEach { it.visibility = measureVisibility }
                nodeSheetBehavior.state = STATE_EXPANDED
                if (sceneBehavior.state != STATE_COLLAPSED) {
                    sceneBehavior.state = STATE_COLLAPSED
                    bottomSheetScene.root.tag = true
                }
            }
            else -> Unit
        }
    }
    
    // ========== ObjectMappingListener Implementation (Simplified) ==========
    
    override fun onObjectMappingUpdate(mappings: List<ObjectMapping>) {
        println("📨 Received object mapping update (not used in simplified delete-previous mode)")
        // No longer needed - we're using simple delete-previous logic
    }
    
    override fun onMappingError(error: String) {
        println("Object mapping error: $error")
        // No action needed for simplified mode
    }
}
