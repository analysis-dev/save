/**
 * File Utils for native platforms
 */

package org.cqfn.save.core.files

import okio.FileSystem

actual val fs: FileSystem = FileSystem.SYSTEM
