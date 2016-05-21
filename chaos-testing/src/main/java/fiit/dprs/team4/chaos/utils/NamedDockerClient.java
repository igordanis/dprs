package fiit.dprs.team4.chaos.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Docker client decorator
 */
public class NamedDockerClient implements DockerClient{

    private DockerClient dockerClient;

    private String name ;

    public NamedDockerClient(DockerClient dockerClient, String name){
        this.dockerClient = dockerClient;
        this.name = name;
    }

    /*
     * Delegated methods
     */

    @Override
    public StartContainerCmd startContainerCmd(String containerId) {
        return dockerClient.startContainerCmd(containerId);
    }

    @Override
    public AuthConfig authConfig() throws DockerException {
        return dockerClient.authConfig();
    }

    @Override
    public AuthCmd authCmd() {
        return dockerClient.authCmd();
    }

    @Override
    public InfoCmd infoCmd() {
        return dockerClient.infoCmd();
    }

    @Override
    public PingCmd pingCmd() {
        return dockerClient.pingCmd();
    }

    @Override
    public VersionCmd versionCmd() {
        return dockerClient.versionCmd();
    }

    @Override
    public PullImageCmd pullImageCmd(String repository) {
        return dockerClient.pullImageCmd(repository);
    }

    @Override
    public PushImageCmd pushImageCmd(String name) {
        return dockerClient.pushImageCmd(name);
    }

    @Override
    public PushImageCmd pushImageCmd(Identifier identifier) {
        return dockerClient.pushImageCmd(identifier);
    }

    @Override
    public CreateImageCmd createImageCmd(String repository, InputStream imageStream) {
        return dockerClient.createImageCmd(repository, imageStream);
    }

    @Override
    public SearchImagesCmd searchImagesCmd(String term) {
        return dockerClient.searchImagesCmd(term);
    }

    @Override
    public RemoveImageCmd removeImageCmd(String imageId) {
        return dockerClient.removeImageCmd(imageId);
    }

    @Override
    public ListImagesCmd listImagesCmd() {
        return dockerClient.listImagesCmd();
    }

    @Override
    public InspectImageCmd inspectImageCmd(String imageId) {
        return dockerClient.inspectImageCmd(imageId);
    }

    @Override
    public SaveImageCmd saveImageCmd(String name) {
        return dockerClient.saveImageCmd(name);
    }

    @Override
    public ListContainersCmd listContainersCmd() {
        return dockerClient.listContainersCmd();
    }

    @Override
    public CreateContainerCmd createContainerCmd(String image) {
        return dockerClient.createContainerCmd(image);
    }

    @Override
    public ExecCreateCmd execCreateCmd(String containerId) {
        return dockerClient.execCreateCmd(containerId);
    }

    @Override
    public InspectContainerCmd inspectContainerCmd(String containerId) {
        return dockerClient.inspectContainerCmd(containerId);
    }

    @Override
    public RemoveContainerCmd removeContainerCmd(String containerId) {
        return dockerClient.removeContainerCmd(containerId);
    }

    @Override
    public WaitContainerCmd waitContainerCmd(String containerId) {
        return dockerClient.waitContainerCmd(containerId);
    }

    @Override
    public AttachContainerCmd attachContainerCmd(String containerId) {
        return dockerClient.attachContainerCmd(containerId);
    }

    @Override
    public ExecStartCmd execStartCmd(String containerId) {
        return dockerClient.execStartCmd(containerId);
    }

    @Override
    public InspectExecCmd inspectExecCmd(String execId) {
        return dockerClient.inspectExecCmd(execId);
    }

    @Override
    public LogContainerCmd logContainerCmd(String containerId) {
        return dockerClient.logContainerCmd(containerId);
    }

    @Override
    public CopyFileFromContainerCmd copyFileFromContainerCmd(String containerId, String resource) {
        return dockerClient.copyFileFromContainerCmd(containerId, resource);
    }

    @Override
    public ContainerDiffCmd containerDiffCmd(String containerId) {
        return dockerClient.containerDiffCmd(containerId);
    }

    @Override
    public StopContainerCmd stopContainerCmd(String containerId) {
        return dockerClient.stopContainerCmd(containerId);
    }

    @Override
    public KillContainerCmd killContainerCmd(String containerId) {
        return dockerClient.killContainerCmd(containerId);
    }

    @Override
    public RestartContainerCmd restartContainerCmd(String containerId) {
        return dockerClient.restartContainerCmd(containerId);
    }

    @Override
    public CommitCmd commitCmd(String containerId) {
        return dockerClient.commitCmd(containerId);
    }

    @Override
    public BuildImageCmd buildImageCmd() {
        return dockerClient.buildImageCmd();
    }

    @Override
    public BuildImageCmd buildImageCmd(File dockerFileOrFolder) {
        return dockerClient.buildImageCmd(dockerFileOrFolder);
    }

    @Override
    public BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        return dockerClient.buildImageCmd(tarInputStream);
    }

    @Override
    public TopContainerCmd topContainerCmd(String containerId) {
        return dockerClient.topContainerCmd(containerId);
    }

    @Override
    public TagImageCmd tagImageCmd(String imageId, String repository, String tag) {
        return dockerClient.tagImageCmd(imageId, repository, tag);
    }

    @Override
    public PauseContainerCmd pauseContainerCmd(String containerId) {
        return dockerClient.pauseContainerCmd(containerId);
    }

    @Override
    public UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        return dockerClient.unpauseContainerCmd(containerId);
    }

    @Override
    public EventsCmd eventsCmd() {
        return dockerClient.eventsCmd();
    }

    @Override
    public StatsCmd statsCmd() {
        return dockerClient.statsCmd();
    }

    @Override
    public void close() throws IOException {
        dockerClient.close();
    }


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
