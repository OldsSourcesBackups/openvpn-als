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

import org.apache.commons.vfs.Capability;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;

/**
 * Absolute URI test cases.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class UriTests
    extends AbstractProviderTestCase
{
    /**
     * Returns the capabilities required by the tests of this test case.
     */
    protected Capability[] getRequiredCaps()
    {
        return new Capability[]{Capability.URI};
    }

    /**
     * Tests resolution of absolute URI.
     */
    public void testAbsoluteURI() throws Exception
    {
        final FileObject readFolder = getReadFolder();

        // Try fetching base folder again by its URI
        final String uri = readFolder.getName().getURI();
        FileObject file = getManager().resolveFile(uri, readFolder.getFileSystem().getFileSystemOptions());
        assertSame("file object", readFolder, file);

        // Try fetching the filesystem root by its URI
        final String rootUri = readFolder.getName().getRootURI();
        file = getManager().resolveFile(rootUri, readFolder.getFileSystem().getFileSystemOptions());
        assertSame(readFolder.getFileSystem().getRoot(), file);
        assertEquals(rootUri, file.getName().getRootURI());
        assertEquals(rootUri, file.getName().getURI());
        assertEquals(FileName.ROOT_PATH, file.getName().getPath());
    }
}
