<!---
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. See accompanying LICENSE file.
-->

# A Model of a Hadoop Filesystem

<!-- MACRO{toc|fromDepth=1|toDepth=3} -->

## Paths and Path Elements

A Path is a list of Path elements which represents a path to a file, directory or symbolic link

Path elements are non-empty strings. The exact set of valid strings MAY
be specific to a particular FileSystem implementation.

Path elements MUST NOT be in `{"", ".",  "..", "/"}`.

Path elements MUST NOT contain the characters `{'/', ':'}`.

Filesystems MAY have other strings that are not permitted in a path element.

When validating path elements, the exception `InvalidPathException` SHOULD
be raised when a path is invalid [HDFS]

### Predicates and Functions

#### `valid-path-element(List[String]): bool`

A path element `pe` is invalid if any character in it is in the set of forbidden characters,
or the element as a whole is invalid

    forall e in pe: not (e in {'/', ':'})
    not pe in {"", ".",  "..", "/"}


#### `valid-path(List[PathElement]): bool`

A Path `p` is *valid* if all path elements in it are valid

    def valid-path(path): forall pe in path: valid-path-element(pe)


The set of all possible paths is *Paths*; this is the infinite set of all lists of valid path elements.

The path represented by empty list, `[]` is the *root path*, and is denoted by the string `"/"`.


#### `parent(path:Path): Path`

The partial function `parent(path:Path):Path` provides the parent path can be defined using
list slicing.

    def parent(pe) : pe[0:-1]

Preconditions:

    path != []


#### `filename(Path): PathElement`

The last Path Element in a Path is called the filename.

    def filename(p) : p[-1]

Preconditions:

    p != []

#### `childElements(Path p, Path q): Path`


The partial function `childElements:(Path p, Path q):Path`
is the list of path elements in `p` that follow the path `q`.

    def childElements(p, q): p[len(q):]

Preconditions:


    # The path 'q' must be at the head of the path 'p'
    q == p[:len(q)]


#### `ancestors(Path): List[Path]`

The list of all paths that are either the direct parent of a path p, or a parent of
ancestor of p.

### Notes for relative paths

This definition handles absolute paths but not relative ones; it needs to be reworked so the root element is explicit, presumably
by declaring that the root (and only the root) path element may be ['/'].

Relative paths can then be distinguished from absolute paths as the input to any function and resolved when the second entry in a two-argument function
such as `rename`.

## Defining the Filesystem


A filesystem `FS` contains directories (a set of paths), files (a mapping of a path to a list of bytes) and symlinks (a set of paths mapping to paths)

    (Directories:Set[Path], Files:Map[Path:List[byte]], Symlinks:Map[Path:Path])


Accessor functions return the specific element of a filesystem

    def directories(FS)  = FS.Directories
    def files(FS) = FS.Files
    def symlinks(FS) = keys(FS.Symlinks)
    def filenames(FS) = keys(FS.Files)

The entire set of a paths finite subset of all possible Paths, and functions to resolve a path to data, a directory predicate or a symbolic link:

    def paths(FS) = FS.Directories + filenames(FS) + symlinks(FS)

A path is deemed to exist if it is in this aggregate set:

    def exists(FS, p) = p in paths(FS)

The root path, "/", is a directory represented  by the path ["/"], which must always exist in a filesystem.

    def isRoot(p) = p == ["/"].

    forall FS in FileSystems : ["/"] in FS.Directories



### Directory references

A path MAY refer to a directory in a FileSystem:

    isDir(FS, p): p in FS.Directories

Directories may have children, that is, there may exist other paths
in the FileSystem whose path begins with a directory. Only directories
may have children. This can be expressed
by saying that every path's parent must be a directory.

It can then be declared that a path has no parent in which case it is the root directory,
or it MUST have a parent that is a directory:

    forall p in paths(FS) : isRoot(p) or isDir(FS, parent(p))

Because the parent directories of all directories must themselves satisfy
this criterion, it is implicit that only leaf nodes may be files or symbolic links:

Furthermore, because every filesystem contains the root path, every filesystem
must contain at least one directory.

A directory may have children:

    def children(FS, p) = {q for q in paths(FS) where parent(q) == p}

There are no duplicate names in the child paths, because all paths are
taken from the set of lists of path elements. There can be no duplicate entries
in a set, hence no children with duplicate names.

A path *D* is a descendant of a path *P* if it is the direct child of the
path *P* or an ancestor is a direct child of path *P*:

    def isDescendant(P, D) = parent(D) == P or isDescendant(P, parent(D))

The descendants of a directory P are all paths in the filesystem whose
path begins with the path P, i.e. their parent is P or an ancestor is P

    def descendants(FS, D) = {p for p in paths(FS) where isDescendant(D, p)}


### File references

A path MAY refer to a file that has data in the filesystem; its path is a key in the data dictionary

    def isFile(FS, p) =  p in keys(FS.Files)


### Symbolic references

A path MAY refer to a symbolic link:

    def isSymlink(FS, p) = p in symlinks(FS)


### File Length

Files store data:

    def data(FS, p) = files(FS)[p]

The length of a path p in a filesystem FS is the length of the data stored, or 0 if it is a directory:

    def length(FS, p) = if isFile(p) : return length(data(FS, p)) else return 0

### User home

The home directory of a user is an implicit part of a filesystem, and is derived from the userid of the
process working with the filesystem:

    def getHomeDirectory(FS) : Path

The function `getHomeDirectory` returns the home directory for the Filesystem and the current user account.
For some FileSystems, the path is `["/","users", System.getProperty("user-name")]`. However,
for HDFS, the username is derived from the credentials used to authenticate the client with HDFS.
This may differ from the local user account name.


### Exclusivity

A path cannot refer to more than one of a file, a directory or a symbolic link


    directories(FS) ^ filenames(FS) == {}
    directories(FS) ^ symlinks(FS) == {}
    filenames(FS) ^ symlinks(FS) == {}


This implies that only files may have data.

This condition is invariant and is an implicit postcondition of all
operations that manipulate the state of a FileSystem `FS`.


### Encryption Zone

The data is encrypted if the file is in encryption zone.

    def inEncryptionZone(FS, path): bool

The nature of the encryption and the mechanism for creating an encryption zone
are implementation details not covered in this specification.
No guarantees are made about the quality of the encryption.
The metadata is not encrypted.

All files and directories under a directory in an encryption zone are also in an
encryption zone.

    forall d in directories(FS): inEncyptionZone(FS, d) implies
      forall c in children(FS, d) where (isFile(FS, c) or isDir(FS, c)) :
        inEncyptionZone(FS, c)

For all files in an encrypted zone, the data is encrypted, but the encryption
type and specification are not defined.

    forall f in files(FS) where inEncyptionZone(FS, f):
      isEncrypted(data(FS, f))


## Notes

Not covered: hard links in a FileSystem. If a FileSystem supports multiple
references in *paths(FS)* to point to the same data, the outcome of operations
are undefined.

This model of a FileSystem is sufficient to describe all the FileSystem
queries and manipulations excluding metadata and permission operations.
The Hadoop `FileSystem` and `FileContext` interfaces can be specified
in terms of operations that query or change the state of a FileSystem.
