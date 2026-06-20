package de.schrell.cadas.application.exchange;

import de.schrell.cadas.domain.model.ProjectModel;
import java.io.IOException;
import java.nio.file.Path;

public interface ProjectExchangeService {

    void exportProject(ProjectModel project, Path targetFile) throws IOException;

    ProjectModel importProject(Path sourceFile, String projectName) throws IOException;
}
