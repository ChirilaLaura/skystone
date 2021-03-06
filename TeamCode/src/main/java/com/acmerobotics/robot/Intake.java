package com.acmerobotics.robot;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

@Config
public class Intake {

    private DcMotorEx leftMotor, rightMotor;
    private Servo leftServo, rightServo;

    private double leftOpen = 0.41;
    private double leftClose = 1;

    private double rightOpen = 0.84;
    private double rightClose = 0.08;

    private double LfullyOpen = 0.24;
    private double RfullyOpen = 1;

    public Intake(HardwareMap hardwareMap){
        leftMotor = hardwareMap.get(DcMotorEx.class, "leftMotor");
        rightMotor = hardwareMap.get(DcMotorEx.class, "rightMotor");

        leftServo = hardwareMap.get(Servo.class, "leftServo");
        rightServo = hardwareMap.get(Servo.class, "rightServo");

        rightMotor.setDirection(DcMotorEx.Direction.REVERSE);

    }


    public void internalSetVelocity(double leftIntakePower, double rightIntakePower){
        leftMotor.setPower(leftIntakePower);
        rightMotor.setPower(rightIntakePower);
    }

    public void leftOpen(){
        leftServo.setPosition(leftOpen);
    }

    public void rightOpen(){
        rightServo.setPosition(rightOpen);
    }

    public void leftClose(){
        leftServo.setPosition(leftClose);
    }

    public void rightClose(){
        rightServo.setPosition(rightClose);

    }

    public void setIntakePower(double intakePower) {
        internalSetVelocity(intakePower, intakePower);
    }

    public void leftFullyOpen(){
        leftServo.setPosition(LfullyOpen);
    }

    public void rightFullyOpen(){
        rightServo.setPosition(RfullyOpen);
    }



}
