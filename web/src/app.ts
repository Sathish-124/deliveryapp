// Minimal TypeScript Web Viewer

// Interface for Frame Data
interface FrameData {
    width: number;
    height: number;
    timestamp: number;
    imageDataBase64: string; // Placeholder for base64 image data
}

class EdgeDetectorViewer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D | null;
    private statsElement: HTMLElement | null;

    constructor(canvasId: string, statsId: string) {
        this.canvas = document.getElementById(canvasId) as HTMLCanvasElement;
        this.ctx = this.canvas.getContext('2d');
        this.statsElement = document.getElementById(statsId);
        
        this.init();
    }

    private init() {
        console.log("Initializing Viewer...");
        // Simulate receiving a frame
        this.simulateFrameReception();
    }

    public updateFrame(data: FrameData) {
        if (!this.ctx || !this.statsElement) return;

        // Update Stats
        this.statsElement.innerText = `Resolution: ${data.width}x${data.height} | Timestamp: ${data.timestamp}`;

        // Render Image
        const img = new Image();
        img.onload = () => {
            this.canvas.width = data.width;
            this.canvas.height = data.height;
            this.ctx?.drawImage(img, 0, 0);
        };
        img.src = data.imageDataBase64;
    }

    private simulateFrameReception() {
        // Create a dummy pattern (checkerboard) as base64
        const width = 320;
        const height = 240;
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (ctx) {
            ctx.fillStyle = '#000';
            ctx.fillRect(0, 0, width, height);
            ctx.strokeStyle = '#FFF';
            ctx.lineWidth = 2;
            ctx.beginPath();
            // Draw some dummy edges
            for(let i=0; i<width; i+=20) {
                ctx.moveTo(i, 0);
                ctx.lineTo(i, height);
            }
            ctx.stroke();
            
            // Text
            ctx.font = "20px Arial";
            ctx.fillStyle = "white";
            ctx.fillText("Dummy Edge Data", 50, 50);
        }

        const dummyData: FrameData = {
            width: width,
            height: height,
            timestamp: Date.now(),
            imageDataBase64: canvas.toDataURL()
        };

        this.updateFrame(dummyData);
    }
}

// Initialize on load
window.addEventListener('DOMContentLoaded', () => {
    new EdgeDetectorViewer('viewerCanvas', 'stats');
});
