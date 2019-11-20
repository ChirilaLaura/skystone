package com.acmerobotics.robot;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.robomatic.util.PIDController;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.acmerobotics.robot.Lift;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.system.SystemProperties;


@Config
public class PlacingArm {

    //TODO add feedforward method and add feedforward to RUN_TO_POSITION

    //ToDo talk about angle issue (resting arm angle and desired arm positions)

    //ToDo find the vales to all the empty variables

    public static double ARM_LENGTH = 15.375;

    public static double ARM_INIT;
    public static double ARM_INTAKE;
    public static double ARM_RELOCATION ;

    public static double ARM_MASS = 0;

    public static double initAngle;
    public static double intakeAngle;
    public static double relocationAngle;
    public static double restingAngle = 0; //angle where motor is doing nothing and the arm is resting

    public static double wantInitAngle = 0; //find angle
    public static double wantIntakeAngle = 0; //find angle
    public static double wantRelocationAngle = 20;

    public static double RADIUS = 0;

    private double startTime;
    private double error;
    private double correction;
    private double targetPosition;

    private static double WHEEL_FROM_CENTER = 0; /////////////////find length of wheel from center

    private static final double TICK_COUNT = 280; //should be 1440 or something
    private static final double DIAMETER = 1; //find real diameter
    private static final double TICKS_PER_INCH = TICK_COUNT/ DIAMETER * Math.PI; //figure out if drive gear reduction is needed

    private DcMotorEx armMotor;
    private Servo handServo;

    private double handOpenPos = 0; //add angle position at which hand will open
    private double handClosePos = 0; // add angle position at which hand will close

    private PIDController pidController;

    public double offset;

    public static double P = 0;
    public static double I = 0;
    public static double D = 0;
    public static double G = 0;

    private enum ArmMode{
        HOLD_POSITION,
        RUN_TO_POSITION,
        DRIVER_CONTROLLED,

    }

    private ArmMode armMode = ArmMode.DRIVER_CONTROLLED;

    public PlacingArm(HardwareMap hardwareMap){

        armMotor = hardwareMap.get(DcMotorEx.class, "Arm Motor");
        pidController = new PIDController(P, I, D);
        handServo = hardwareMap.get(Servo.class, "hand Servo");


        armMotor.setDirection(DcMotorEx.Direction.FORWARD);
        armMotor.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        armMotor.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);


    }

    public void resetEncoder(){
        armMotor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);


    }

    public double checkEncoder(){
        return armMotor.getCurrentPosition();

    }


    public double getPosition(){
        return internalGetPosition() + offset;

    }

    public double internalGetPosition(){
        return ((armMotor.getCurrentPosition() / (armMotor.getMotorType().getTicksPerRev())) * Math.PI * RADIUS * 2);

    }

    public void setPower(double power){
        internalSetVelocity(power);

    }

    public void setPosition(double position){
        offset = position - internalGetPosition();

    }

    public void internalSetVelocity(double v){
        armMotor.setPower(v);

    }

    public void update(TelemetryPacket packet){// telemetry seems to only be used in teleOP. So is
                                               // telemetryPacket like teleOP but made for code outside TeleOp class?
        packet.put("arm mode", armMode.toString());
        packet.put("position", getPosition());

        switch (armMode){
            case HOLD_POSITION:
                error = targetPosition - armMotor.getCurrentPosition() ; //find out what to do here now
                packet.put("error", error);

                correction = pidController.update(error);
                internalSetVelocity(correction);////////////////////might need to make correction negative or motor should be reversed
                packet.put("arm correction", correction);

                break;


            case RUN_TO_POSITION:
                pidController = new PIDController(P, I, D);
                double t = System.currentTimeMillis() - startTime/ 1000;// start time is used as move the motion state so it is out of its 0 or motion state start position
                                                                        // that way pid won't get a 0 error when not at set point. Start time skips over motion state start
                                                                        //position to not confuse pid (only pid sees a skipped motion state start position).

                    error = targetPosition - armMotor.getCurrentPosition();
                    packet.put("error", error);
                    correction = pidController.update(error);

                    internalSetVelocity(correction); //add feedforward
                    packet.put("arm correction", correction);/////correction might have to be changed to negative or motor should be reversed


                    if(t > 1){ //how you will know you reached the destination (probably use encoders or time) (the current 2 second time is just so A-S won't through an error)
                        packet.put("complete", true);

                        armMode = armMode.HOLD_POSITION;
                        //return;  ???


                        armMode = ArmMode.HOLD_POSITION;
                    //return; ???????

                }


               break;

        }
    }


    public void setMotorEncoders(double distance) {
        int moveMotorTo = armMotor.getCurrentPosition() + convertToTicks(distance);
        armMotor.setTargetPosition(moveMotorTo);
        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);// figure out the difference of run to position from ArmMode and DcMotor.RunMode
    }

    public int convertToTicks(double distance){
        int numToTicks = (int)(distance * TICKS_PER_INCH);
        return numToTicks;
    }

    public void goToPosition(double position){
       // armMode = ArmMode.RUN_TO_POSITION;
        internalSetVelocity(1);

        startTime = System.currentTimeMillis();
        setMotorEncoders(position);
        targetPosition = armMotor.getCurrentPosition() + convertToTicks(position);

    }

    public void armGoToIntake(){

        armMode = ArmMode.RUN_TO_POSITION;
        pidController = new PIDController(P, I, D);
    }

    public void armInitPosition(){
        //Radian of init is calculated. Motion profiling and pid are initialized. Arm moves to init position.

        initAngle = getActualAngle(wantInitAngle, restingAngle);

        ARM_INIT = getRadianLen(initAngle, ARM_LENGTH);

        goToPosition(ARM_INIT);
        pidController = new PIDController(P, I, D);

    }

    public void armIntakePosition(){
        //Radian of intake position is calculated. Motion profiling and pid are initialized. Arm moves to intake position.

        intakeAngle = getActualAngle(wantIntakeAngle, restingAngle);

        ARM_INTAKE = getRadianLen(intakeAngle, ARM_LENGTH);

        goToPosition(ARM_INTAKE);
        pidController = new PIDController(P, I, D);
    }

    public void armRelocationPosition(){
        //Arm angle is set to 90 degrees. Motion profiling and pid are initialized. Arm moves to relocation position

        relocationAngle = getActualAngle(wantRelocationAngle, restingAngle);

        ARM_RELOCATION = getRadianLen(relocationAngle, ARM_LENGTH);

        goToPosition(ARM_RELOCATION);
        pidController = new PIDController(P, I, D);

    }

    public double getRadianLen(double angle, double radius){
        // returns radian length (arm movement curve length)

        double i =  (angle/360) * 2 * Math.PI * radius;
        return i;
    }

    public void setServo(String position){
        // take in close or open then set servo position accordingly

        if (position.equals("open")){
            //open hand
            handServo.setPosition(handOpenPos);
        }

        if (position.equals("close")){
            //close hand
            handServo.setPosition(handClosePos);
        }
    }

    public double getActualAngle(double angle, double restingAngle){
        // angle is the angle you want the arm to be placed when the lift is angle 0. a is the angle that will actual work with goToPosition
        // look at "Actual Angel" paper for details.
        double a = angle - restingAngle;
        return a;
    }

}