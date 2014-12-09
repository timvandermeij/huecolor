This repository contains HueColor, an Android app that can convert an image 
(either loaded from the device itself or taken by the device's built-in camera) 
to grayscale and let the user interactively select a part of the image to 
convert back to color. The user highlights this part with his or her fingers 
and therefore the selection can be rather imprecise. To address this issue, we 
implement edge detection to `fix' the user's selection. The user is also able 
to apply other photo effects using this app.

Prerequisites
=============

In order to be able to compile the app, it is required to download and install 
[Android Studio](https://developer.android.com/sdk/index.html) which is 
available for all major operating systems. To summarize, the following tools 
are required.

* Git 1.9 or higher
* Android Studio 0.8 or higher

Cloning the repository
======================

The first step is to clone the repository to obtian a local copy of the code. Open a terminal window and run the following commands.

    $ git clone https://github.com/timvandermeij/huecolor.git
    $ cd huecolor

Compiling the app
=================

Open the project file with Android Studio, connect a device (either an emulator 
or a real device) and press the Play button in the toolbar. The app will 
automatically be installed on the device and run.

Be sure to enable USB debugging and development options on the device!

Authors
=======

* Tim van der Meij (Leiden University, @timvandermeij)
* Simon Klaver (Leiden University, @Justist)
* Leon Helwerda (Leiden University, @lhelwerd)

License
=======

HueColor is licensed under the Apache 2.0 license. See [the terms and 
conditions](http://www.apache.org/licenses/LICENSE-2.0) for more details.
