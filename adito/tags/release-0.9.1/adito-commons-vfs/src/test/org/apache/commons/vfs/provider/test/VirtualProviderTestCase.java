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
package org.apache.commons.vfs.provider.test;

import junit.framework.Test;
import org.apache.commons.AbstractVfsTestCase;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.test.AbstractProviderTestConfig;
import org.apache.commons.vfs.test.ProviderTestSuite;

import java.io.File;

/**
 * Test cases for the virtual file system provider.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class VirtualProviderTestCase
    extends AbstractProviderTestConfig
{
    public static Test suite() throws Exception
    {
        final ProviderTestSuite testSuite = new ProviderTestSuite(new VirtualProviderTestCase());
        testSuite.addTests(JunctionTests.class);
        return testSuite;
    }

    /**
     * Returns the base folder for tests.
     */
    public FileObject getBaseTestFolder(final FileSystemManager manager) throws Exception
    {
        final File baseDir = AbstractVfsTestCase.getTestDirectoryFile();
        final FileObject baseFile = manager.toFileObject(baseDir);
        return manager.createVirtualFileSystem(baseFile);
    }
}
