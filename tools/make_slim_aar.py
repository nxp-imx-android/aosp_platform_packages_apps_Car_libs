#!/usr/bin/env python3
#
# Copyright 2021, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from argparse import ArgumentParser as AP
from zipfile import ZipFile, ZIP_DEFLATED
import os
import re

def main():
    parser = AP(description='AARs built with soong currently have classes from all their dependencies '
                            'included inside them, and no resources. This tool takes such an AAR, removes'
                            'the dependency classes, and adds resources')
    parser.add_argument('--classes-allowlist', default='')
    parser.add_argument('output')
    parser.add_argument('soong_aar')
    parser.add_argument('res_folders', nargs='*')
    args = parser.parse_args()

    if (len(args.classes_allowlist) <= 0):
        raise ValueError("--classes-allowlist must not be empty")

    with ZipFile(args.output, mode='w', compression=ZIP_DEFLATED) as outaar, ZipFile(args.soong_aar) as soongaar:
        # Create a new classes.jar that only has the classes with the desired prefix
        with soongaar.open('classes.jar') as soongclasses, outaar.open('classes.jar', mode='w') as outclasses:
            with ZipFile(soongclasses) as soongclasseszip, ZipFile(outclasses, mode='w', compression=ZIP_DEFLATED) as outclasseszip:
                # R classes shouldn't be included, they will be regenerated based on the R.txt
                # when building the final app.
                for c in soongclasseszip.namelist():
                    if c.startswith(args.classes_allowlist) and not re.match(r'.+/R(\$[a-zA-Z$]*)?\.class$', c):
                        outclasseszip.writestr(c, soongclasseszip.read(c))

        # Copy all files other than classes.jar from the soong aar to the output aar
        for f in soongaar.namelist():
            if f != 'classes.jar':
                outaar.writestr(f, soongaar.read(f))

        # add the provided resource folders to the output aar
        resIndex=1
        for resfolder in args.res_folders:
            for d in os.listdir(resfolder):
                if os.path.isdir(os.path.join(resfolder, d)):
                    for f in os.listdir(os.path.join(resfolder, d)):
                        path = os.path.join(resfolder, d, f)
                        if d.startswith('values'):
                            # add a number to the end of all the files in the values folder,
                            # that increases with each res folder we add. This allows us to
                            # include files with the same name in the aar.
                            if f.endswith('.xml'):
                                f = f[:-4] + str(resIndex) + '.xml'
                            else:
                                f = f + str(resIndex)
                        outaar.write(path, arcname=os.path.join('res', d, f))
            resIndex += 1


if __name__ == "__main__":
    main()
