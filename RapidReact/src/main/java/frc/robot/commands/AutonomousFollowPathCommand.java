package frc.robot.commands;

import com.pathplanner.lib.PathPlannerTrajectory;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.config.Config;
import frc.robot.log.BucketLog;
import frc.robot.log.LogLevel;
import frc.robot.log.Loggable;
import frc.robot.log.Put;
import frc.robot.subsystem.AutonomousSubsystem;
import frc.robot.subsystem.DrivetrainSubsystem;
import frc.robot.subsystem.RGBSubsystem;

public class AutonomousFollowPathCommand extends SequentialCommandGroup
{
    private final PathPlannerTrajectory trajectory;
    private AutonomousSubsystem auto;
    private DrivetrainSubsystem drive;
    private RGBSubsystem rgb;
    private Config.AutonomousConfig autoConfig;

    private final Loggable<String> state = BucketLog.loggable(Put.STRING, "auto/followPathState");

    public AutonomousFollowPathCommand(PathPlannerTrajectory trajectory, AutonomousSubsystem auto, DrivetrainSubsystem drive, RGBSubsystem rgb)
    {
        this.autoConfig = new Config().auto;

        this.trajectory = trajectory;
        this.auto = auto;
        this.drive = drive;
        this.rgb = rgb;

        this.addCommands(this.setup(), this.createTrajectoryFollowerCommand(), this.setDown());
    }

    private CustomPPSwerveControllerCommand createTrajectoryFollowerCommand()
    {
        PIDController xController = new PIDController(this.autoConfig.pathXYPID.getKP(), this.autoConfig.pathXYPID.getKI(), this.autoConfig.pathXYPID.getKD());
        PIDController yController = new PIDController(this.autoConfig.pathXYPID.getKP(), this.autoConfig.pathXYPID.getKI(), this.autoConfig.pathXYPID.getKD());
        ProfiledPIDController thetaController = new ProfiledPIDController(this.autoConfig.pathThetaPID.getKP(), this.autoConfig.pathThetaPID.getKI(), this.autoConfig.pathThetaPID.getKD(), new TrapezoidProfile.Constraints(this.drive.getMaxAngularVelocity(), this.drive.getMaxAngularVelocity() * 10.0));
        thetaController.enableContinuousInput(-Math.PI, Math.PI);

        return new CustomPPSwerveControllerCommand(
                this.trajectory, //Trajectory
                () -> this.drive.odometry.getPoseMeters(), //Robot Pose supplier
                this.drive.kinematics, //Swerve Drive Kinematics
                xController, //PID Controller: X
                yController, //PID Controller: Y
                thetaController, //PID Controller: Θ
                this.drive::setStates, //SwerveModuleState setter
                this.drive
        );
    }

    private InstantCommand setup()
    {
        return new InstantCommand(() ->{
            this.state.log(LogLevel.GENERAL, "Starting to Follow a Trajectory!");

            //this.drive.zeroStates(new Pose2d(this.trajectory.getInitialState().poseMeters.getTranslation(), this.trajectory.getInitialState().holonomicRotation));

            this.rgb.autoDriving();
        });
    }

    private InstantCommand setDown()
    {
        return new InstantCommand(() -> {
            this.state.log(LogLevel.GENERAL, "Finished Following a Trajectory!");

            this.rgb.autoNotDriving();
        });
    }
}
