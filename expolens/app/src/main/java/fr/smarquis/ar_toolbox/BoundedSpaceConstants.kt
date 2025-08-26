package fr.smarquis.ar_toolbox

/**
 * Constants for configuring bounded 3D spaces around AR anchors
 * 
 * All dimensions are in meters. Adjust these values to change how large
 * models can be within their bounded spaces.
 */
object BoundedSpaceConstants {
    
    // =========================
    // BOUNDED SPACE DIMENSIONS
    // =========================
    
    /** Default bounded space size for most AR objects (30cm cubic space) */
    const val DEFAULT_BOUNDED_SPACE_SIZE = 0.3f
    
    /** Bounded space size for basic geometric shapes (Sphere, Cube, Cylinder) */
    const val BASIC_SHAPES_BOUNDED_SPACE_SIZE = 0.25f
    
    /** Bounded space size for Andy/Cat model (40cm cubic space) */
    const val ANDY_CAT_BOUNDED_SPACE_SIZE = 0.4f
    
    /** Bounded space size for external Link models (50cm cubic space) */
    const val EXTERNAL_LINK_BOUNDED_SPACE_SIZE = 0.5f
    
    /** Bounded space size for Drawing/Doodle objects */
    const val DRAWING_BOUNDED_SPACE_SIZE = 0.2f
    
    /** Bounded space size for Layout/UI elements */
    const val LAYOUT_BOUNDED_SPACE_SIZE = 0.35f
    
    /** Bounded space size for Video objects */
    const val VIDEO_BOUNDED_SPACE_SIZE = 0.45f
    
    /** Bounded space size for CloudAnchor objects */
    const val CLOUD_ANCHOR_BOUNDED_SPACE_SIZE = 0.15f
    
    /** Bounded space size for Augmented Image objects */
    const val AUGMENTED_IMAGE_BOUNDED_SPACE_SIZE = 0.6f
    
    // =========================
    // SCALING BEHAVIOR
    // =========================
    
    /** Whether to allow scaling up small models to fit the bounded space */
    const val ALLOW_SCALE_UP = false
    
    /** Maximum scale factor allowed (prevents models from becoming too large) */
    const val MAX_SCALE_FACTOR = 2.0f
    
    /** Minimum scale factor allowed (prevents models from becoming too small) */
    const val MIN_SCALE_FACTOR = 0.1f
    
    // =========================
    // FALLBACK VALUES
    // =========================
    
    /** Default model size assumption when no collision shape is available */
    const val ASSUMED_DEFAULT_MODEL_SIZE = 0.5f
    
    /** Conservative model size assumption for unknown collision shapes */
    const val ASSUMED_LARGE_MODEL_SIZE = 1.0f
    
    // =========================
    // QUICK PRESETS
    // =========================
    
    /** Extra small bounded space (10cm) - for tiny decorative objects */
    const val EXTRA_SMALL_SPACE = 0.1f
    
    /** Small bounded space (20cm) - for small objects like tools, jewelry */
    const val SMALL_SPACE = 0.2f
    
    /** Medium bounded space (40cm) - for medium objects like furniture pieces */
    const val MEDIUM_SPACE = 0.4f
    
    /** Large bounded space (60cm) - for large objects like vehicles, buildings */
    const val LARGE_SPACE = 0.6f
    
    /** Extra large bounded space (80cm) - for very large objects */
    const val EXTRA_LARGE_SPACE = 0.8f
}
