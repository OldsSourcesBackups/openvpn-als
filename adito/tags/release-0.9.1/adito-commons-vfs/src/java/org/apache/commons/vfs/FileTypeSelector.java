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
package org.apache.commons.vfs;

/**
 * A {@link FileSelector} that selects files of a particular type.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class FileTypeSelector
    implements FileSelector
{
    private final FileType type;

    public FileTypeSelector(final FileType type)
    {
        this.type = type;
    }

    /**
     * Determines if a file or folder should be selected.
     */
    public boolean includeFile(final FileSelectInfo fileInfo)
        throws FileSystemException
    {
        return (fileInfo.getFile().getType() == type);
    }

    /**
     * Determines whether a folder should be traversed.
     */
    public boolean traverseDescendents(final FileSelectInfo fileInfo)
    {
        return true;
    }
}
