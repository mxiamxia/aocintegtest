package com.amazon.aocagent.installers.otinstallers;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.fileconfigs.EcsEc2Template;
import com.amazon.aocagent.fileconfigs.EcsFargateTemplate;
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.EcsContext;
import com.amazon.aocagent.services.ECSService;
import com.amazon.aocagent.services.IAMService;
import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.NetworkConfiguration;
import com.amazonaws.services.ecs.model.RunTaskRequest;

public class EcsInstaller implements OTInstaller {
  private Context context;
  private ECSService ecsService;
  private IAMService iamService;
  private MustacheHelper mustacheHelper;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.ecsService = new ECSService(context.getStack().getTestingRegion());
    this.iamService = new IAMService(context.getStack().getTestingRegion());
    this.mustacheHelper = new MustacheHelper();
  }

  @Override
  public void installAndStart() throws Exception {

    this.setupEcsContext(context);

    // create and run ECS sidecar the target task definitions from template
    final String taskDefinitionStr = this.getTaskDefinition(this.context);

    // register the task definition in ECS
    ecsService.registerTaskDefinition(taskDefinitionStr);

    // create ecs run task request
    RunTaskRequest taskRequest = this.getTaskRequest(this.context);

    // run ECS task
    ecsService.runTaskDefinition(taskRequest);
  }

  private void setupEcsContext(Context context) {
    EcsContext ecsContext = context.getEcsContext();
    ecsContext.setAocImage(GenericConstants.AOC_IMAGE.getVal() + context.getAgentVersion());
    ecsContext.setDataEmitterImage(GenericConstants.TRACE_EMITTER_DOCKER_IMAGE_URL.getVal());
    // ECS uses current timestamp as instance id
    ecsContext.setInstanceId(String.valueOf(System.currentTimeMillis()));
    ecsContext.setRegion(context.getStack().getTestingRegion());
    String iamRoleArn = this.iamService.getRoleArn(GenericConstants.IAM_ROLE_NAME.getVal());
    ecsContext.setTaskRoleArn(iamRoleArn);
    ecsContext.setExecutionRoleArn(iamRoleArn);
  }

  private RunTaskRequest getTaskRequest(Context context) {
    String launchType = context.getEcsContext().getLaunchType();
    if (launchType.equalsIgnoreCase(GenericConstants.EC2.getVal())) {
      return new RunTaskRequest()
              .withLaunchType(LaunchType.EC2)
              .withTaskDefinition(GenericConstants.AOC_PREFIX.getVal() + launchType)
              .withCluster(context.getEcsContext().getClusterName())
              .withCount(1);
    } else {
      return new RunTaskRequest()
              .withLaunchType(LaunchType.FARGATE)
              .withTaskDefinition(GenericConstants.AOC_PREFIX.getVal() + launchType)
              .withCluster(context.getEcsContext().getClusterName())
              .withCount(1)
              .withNetworkConfiguration(
                  new NetworkConfiguration()
                      .withAwsvpcConfiguration(
                          new AwsVpcConfiguration()
                              .withAssignPublicIp(AssignPublicIp.ENABLED)
                              .withSecurityGroups(context.getDefaultSecurityGrpId())
                              .withSubnets(context.getDefaultSubnets().get(0).getSubnetId())));
    }
  }

  private String getTaskDefinition(Context context) throws BaseException {
    String launchType = context.getEcsContext().getLaunchType();
    try {
      if (launchType.equalsIgnoreCase(GenericConstants.EC2.getVal())) {
        return mustacheHelper.render(EcsEc2Template.ECS_EC2_TEMPLATE, context.getEcsContext());
      } else {
        return mustacheHelper.render(EcsFargateTemplate.ECS_FARGATE_TEMPLATE, context.getEcsContext());
      }
    } catch (Exception e) {
      throw new BaseException(ExceptionCode.ECS_TASK_EXECUTION_FAIL, e.getMessage());
    }
  }
}