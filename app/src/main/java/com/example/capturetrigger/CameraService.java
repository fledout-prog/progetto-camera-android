package com.example.capturetrigger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import java.util.Collections;

public class CameraService extends Service {
    private static final String TAG = "CameraService_Debug";
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Surface mTargetSurface; // La superficie della tua SurfaceView
    private String mCameraId = "0"; // Fotocamera posteriore principale

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    public void onCreate() {
        super.onCreate();
        startBackgroundThread();
    }

    /**
     * Metodo chiamato dalla MainActivity per passare la Surface e avviare tutto
     */
    public void setupCameraPreviewFromSurface(Surface surface) {
        this.mTargetSurface = surface;
        Log.d(TAG, "Surface ricevuta, avvio apertura camera...");
        openCamera();
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Verifichiamo i permessi prima (anche se gestiti in Activity)
            manager.openCamera(mCameraId, stateCallback, mBackgroundHandler);
        } catch (CameraAccessException | SecurityException e) {
            Log.e(TAG, "Errore fatale nell'apertura camera: " + e.getMessage());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Log.d(TAG, "Camera aperta con successo");
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "Camera scollegata");
            stopCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Errore CameraDevice: " + error);
            stopCamera();
        }
    };

    private void createCameraPreviewSession() {
        if (mCameraDevice == null || mTargetSurface == null) {
            Log.e(TAG, "Impossibile creare sessione: camera o surface null");
            return;
        }

        try {
            // Prepariamo la richiesta di anteprima
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mTargetSurface);

            // Creazione della sessione di cattura
            mCameraDevice.createCaptureSession(Collections.singletonList(mTargetSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCameraDevice == null) return;

                            mCaptureSession = session;
                            try {
                                // Focus continuo per evitare immagini sfocate
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // AVVIO REALE DEL FLUSSO VIDEO
                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                                Log.d(TAG, "SESSIONE CONFIGURATA: Il video dovrebbe apparire ora!");
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Errore nel setRepeatingRequest", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Configurazione sessione fallita! Possibile risoluzione non supportata.");
                        }
                    }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Errore creazione CaptureSession", e);
        }
    }

    // Metodo per scattare la foto (collegato al tuo bottone)
    public void capturePhoto() {
        Log.d(TAG, "Richiesta cattura foto ricevuta");
        // Qui andrà la logica di ImageReader per salvare il file
    }

    public void stopCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Errore stop background thread", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopCamera();
        stopBackgroundThread();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}