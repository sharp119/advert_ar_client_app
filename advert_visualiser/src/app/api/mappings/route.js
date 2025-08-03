// src/app/api/mappings/route.js
import { NextResponse } from 'next/server';

// In-memory storage for node mappings
let nodeMappings = new Map();

export async function GET(request) {
  try {
    // Convert Map to array
    const mappingsArray = Array.from(nodeMappings.values());
    
    const response = {
      mappings: mappingsArray,
      timestamp: new Date().toISOString(),
      version: "1.0",
      count: mappingsArray.length
    };
    
    console.log(`📋 Returning ${mappingsArray.length} node mappings`);
    
    return NextResponse.json(response);
  } catch (error) {
    console.error('❌ Error fetching mappings:', error);
    return NextResponse.json(
      { error: 'Failed to fetch mappings' },
      { status: 500 }
    );
  }
}

export async function POST(request) {
  try {
    const mapping = await request.json();
    
    console.log('📝 Adding/updating node mapping:', mapping);
    
    // Validate required fields
    if (!mapping.nodeId || !mapping.modelUrl) {
      return NextResponse.json(
        { error: 'nodeId and modelUrl are required' },
        { status: 400 }
      );
    }
    
    // Store the mapping with nodeId as the key
    nodeMappings.set(mapping.nodeId, {
      nodeId: mapping.nodeId,
      modelUrl: mapping.modelUrl,
      modelName: mapping.modelName || 'Unknown Model',
      timestamp: new Date().toISOString()
    });
    
    return NextResponse.json({ 
      success: true, 
      message: 'Mapping added/updated successfully',
      nodeId: mapping.nodeId
    });
  } catch (error) {
    console.error('❌ Error adding mapping:', error);
    return NextResponse.json(
      { error: 'Failed to add mapping' },
      { status: 500 }
    );
  }
}

export async function DELETE(request) {
  try {
    const url = new URL(request.url);
    const nodeId = url.searchParams.get('nodeId');
    
    if (nodeId) {
      // Delete specific mapping
      if (nodeMappings.has(nodeId)) {
        nodeMappings.delete(nodeId);
        console.log(`🗑️ Deleted mapping for node ${nodeId}`);
        return NextResponse.json({ 
          success: true, 
          message: 'Mapping deleted successfully' 
        });
      } else {
        return NextResponse.json(
          { error: 'Mapping not found' },
          { status: 404 }
        );
      }
    } else {
      // Clear all mappings
      const count = nodeMappings.size;
      nodeMappings.clear();
      console.log(`🗑️ Cleared all ${count} mappings`);
      return NextResponse.json({ 
        success: true, 
        message: `Cleared ${count} mappings` 
      });
    }
  } catch (error) {
    console.error('❌ Error deleting mappings:', error);
    return NextResponse.json(
      { error: 'Failed to delete mappings' },
      { status: 500 }
    );
  }
}