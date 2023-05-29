# FlowerDig

This repository includes the code for:
  - The training of two neural networks for keypoint regression in geometric morphometrics for flowers with 4 petals.
  - An Android app running the model in real time using the camera raw as input.

The data (about 1000 labeled images) used for training has not been included due to its large size. 

The Android app is based on [this](https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android_play_services) example from the Tensorflow Authors. The original License is included. Changes on the original code mainly include:
  - The camera output processing, in order to adjust it to our model input requirements.
  - All the code related to the model loading and execution.
  - The model's output representation and visualization.

The time performance of the models has been tested on a [Redmi Note 11 Pro+ 5G](https://www.mi.com/es/product/redmi-note-11-pro-plus-5g/specs), running both at more than 30 FPS on CPU. The models, stored in TFLite format, are very lightweight (6~8MB). 
