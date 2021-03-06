/*
 * Copyright 2002-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs.test;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FilesCache;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;

/**
 * Test configuration for a file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public interface ProviderTestConfig
{
    /**
     * Prepares the file system manager.
     */
    void prepare(DefaultFileSystemManager manager) throws Exception;

    /**
     * Returns the base folder for tests.  This folder must exist, and contain
     * the following structure:
     * <ul>
     * <li>/read-tests
     * <li>/write-tests
     * </ul>
     */
    FileObject getBaseTestFolder(FileSystemManager manager) throws Exception;

    /**
     * Returns the filesCache implementation used for tests.
     */
    FilesCache getFilesCache();
}
