package a3;

import org.joml.*;
import org.joml.Math;

import java.awt.event.KeyEvent;

public class Camera {
    private Vector3f position;
    private float pitch;
    private float yaw;

    private float moveSpeed;
    private float lookSpeed;

    private Vector3f up;
    private Vector3f right;
    private Vector3f forward;

    private Matrix4f viewMatrix;

    public Camera() {
        position = new Vector3f(0.0f, 1.75f, 5.0f);
        pitch = 0.0f;
        yaw = -(float) Math.PI/2.0f;
        moveSpeed = 5.0f;
        lookSpeed = 0.025f;

        up = new Vector3f(0.0f, 1.0f, 0.0f);
        right = new Vector3f(1.0f, 0.0f, 0.0f);
        forward = new Vector3f(0.0f, 0.0f, -1.0f);

        viewMatrix = new Matrix4f();
        updateVectors();
    }

    private void updateVectors() {
        forward.x = (float)Math.cos(yaw) * (float)Math.cos(pitch);
        forward.y = (float)Math.sin(pitch);
        forward.z = (float)Math.sin(yaw) * (float)Math.cos(pitch);
        forward.normalize();

        right = new Vector3f(forward).cross(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
        up = new Vector3f(right).cross(forward).normalize();
    }

    public void updateViewMatrix() {
        updateVectors();

        // Look-at method using our calculated vectors
        viewMatrix.identity();
        viewMatrix.lookAt(
                position,                        // Camera position
                new Vector3f(position).add(forward), // Point to look at (position + forward)
                up                               // Up vector
        );
    }

    public void handleKeyInput(int keyCode, float deltaTime) {
        float actualMoveSpeed = moveSpeed * deltaTime;

        switch (keyCode) {
            case KeyEvent.VK_W:
                position.add(new Vector3f(forward).mul(actualMoveSpeed));
                break;
            case KeyEvent.VK_S:
                position.add(new Vector3f(forward).mul(-actualMoveSpeed));
                break;
            case KeyEvent.VK_A:
                position.add(new Vector3f(right).mul(-actualMoveSpeed));
                break;
            case KeyEvent.VK_D:
                position.add(new Vector3f(right).mul(actualMoveSpeed));
                break;
            // Vertical movement
            case KeyEvent.VK_Q:
                position.y -= actualMoveSpeed;
                break;
            case KeyEvent.VK_E:
                position.y += actualMoveSpeed;
                break;

            case KeyEvent.VK_UP:
                pitch += lookSpeed;
                break;
            case KeyEvent.VK_DOWN:
                pitch -= lookSpeed;
                break;
            case KeyEvent.VK_LEFT:
                yaw -= lookSpeed;
                break;
            case KeyEvent.VK_RIGHT:
                yaw += lookSpeed;
                break;
        }

        updateViewMatrix();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getForward() {
        return new Vector3f(forward);
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public void setLookSpeed(float lookSpeed) {
        this.lookSpeed = lookSpeed;
    }
}