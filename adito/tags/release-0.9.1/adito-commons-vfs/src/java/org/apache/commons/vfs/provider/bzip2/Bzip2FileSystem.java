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
package org.apache.commons.vfs.provider.bzip2;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.compressed.CompressedFileFileSystem;

import java.util.Collection;

/**
 * Filesytem to handle compressed files using the bzip2 method
 *
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 */
public class Bzip2FileSystem extends CompressedFileFileSystem
{
    protected Bzip2FileSystem(FileName rootName, FileObject parentLayer, FileSystemOptions fileSystemOptions) throws FileSystemException
    {
        super(rootName, parentLayer, fileSystemOptions);
    }

    protected FileObject createFile(FileName name) throws FileSystemException
    {
        return new Bzip2FileObject(name, getParentLayer(), this);
    }

    protected void addCapabilities(final Collection caps)
    {
        caps.addAll(Bzip2FileProvider.capabilities);
    }
}
