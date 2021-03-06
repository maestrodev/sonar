/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Project;

/**
 * <p>
 * Initializer can execute external tool (like a Maven plugin), change project configuration. For example CoberturaMavenInitializer invokes
 * the Codehaus Cobertura Mojo and sets path to Cobertura report according to Maven POM.
 * </p>
 * 
 * <p>
 * Initializers are executed first and once during project analysis.
 * </p>
 * 
 * @since 2.6
 */
public abstract class Initializer implements BatchExtension, CheckProject {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public abstract void execute(Project project);

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
