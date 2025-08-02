// src/app/page.js
import ThreeDGraphViewer from '@/components/ThreeDGraphViewer'; // Adjust path if needed

export default function Home() {
  return (
    <main>
      <h1>AR Device Tracker</h1>
      <p>Live 3D visualization of your AR device's position and orientation.</p>
      <div style={{ position: 'relative', width: '100%', height: 'calc(100vh - 100px)' }}>
        {/* The 3D viewer will take up the remaining height */}
        <ThreeDGraphViewer />
      </div>
    </main>
  );
}