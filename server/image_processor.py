import cv2
import numpy as np
import os

MAX_IMAGES = 4
MIN_IMAGES = 2

cascade_path = r'haarcascades\haarcascade_frontalface_default.xml'
cascade = cv2.CascadeClassifier(cascade_path)

orb_detector = cv2.ORB_create(5000)
matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)


def register(images, base_index):
    """
    Homographically aligns images to the perspective of the base image

    Reference: https://www.geeksforgeeks.org/image-registration-using-opencv-python/

    :param images: Images to align
    :param base_index: Index of the base image
    """
    reference_image_color = images[base_index]
    reference_image = cv2.cvtColor(reference_image_color, cv2.COLOR_BGR2GRAY)
    height, width = reference_image.shape
    for i in range(len(images)):
        if i == base_index:
            continue
        image_color = images[i]
        image = cv2.cvtColor(image_color, cv2.COLOR_BGR2GRAY)
        image_kp, image_d = orb_detector.detectAndCompute(image, None)
        ref_kp, ref_d = orb_detector.detectAndCompute(reference_image, None)
        matches = matcher.match(image_d, ref_d)
        matches.sort(key=lambda x: x.distance)
        matches = matches[:int(len(matches) * 90)]
        no_of_matches = len(matches)
        p1 = np.zeros((no_of_matches, 2))
        p2 = np.zeros((no_of_matches, 2))
        for j in range(len(matches)):
            p1[j, :] = image_kp[matches[j].queryIdx].pt
            p2[j, :] = ref_kp[matches[j].trainIdx].pt
        homography, mask = cv2.findHomography(p1, p2, cv2.RANSAC)
        transformed_img = cv2.warpPerspective(image_color, homography, (width, height))
        images[i] = transformed_img


def get_faces(image):
    """
    Performs Haar-cascade face detection

    :param image: Input image
    :return f: a list of cropped faces detected in the image
    :return b: a list of bounding box coordinates for faces detected
    """
    f, b = [], []
    grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = cascade.detectMultiScale(
        grey,
        scaleFactor=1.1,
        minNeighbors=10,
        minSize=(30, 30),
        flags=cv2.CASCADE_SCALE_IMAGE
    )
    for (x, y, w, h) in faces:
        f.append(image[y:y + h, x:x + w])
        b.append((x, y, w, h))
        # cv2.rectangle(grey, (x, y), (x + w, y + h), (0, 255, 0), 2)
    # cv2.imshow('ImageWindow', grey)
    # cv2.waitKey()
    return f, b


def overlay(image, crop, anchor):
    """
    Stitches an image onto the base image using the given anchor

    :param image: Base image
    :param crop: Image to be stitched
    :param anchor: (x,y,w,h) region in the base image to which to perform the stitching
    :return: Resulting image
    """
    background = image.copy()
    over = cv2.resize(crop, (anchor[2], anchor[3]))
    background[anchor[1]:anchor[1] + anchor[3], anchor[0]:anchor[0] + anchor[2], :] = over

    # src = over.astype('uint8')
    # dst = image.astype('uint8')
    # mask = np.zeros(src.shape).astype('uint8')
    # center = (int(anchor[1] + anchor[3] / 2), int(anchor[0] + anchor[2] / 2))
    # flags = cv2.NORMAL_CLONE
    # background = cv2.seamlessClone(src, dst, mask, center, flags)

    return background


def merge(image, crops, anchors):
    base = image
    for i in range(len(crops)):
        base = overlay(base, crops[i], anchors[i])
    return base


class Imutils:
    ready = False
    output = None
    images = []
    boxes = []
    faces = []
    base_index = 0

    def add_image(self, path):
        if len(self.images) >= MAX_IMAGES:
            print('Exceeded max images')
            return False
        image = cv2.imread(path)
        self.images.append(image)
        self.ready = False
        return True

    def reset(self):
        self.ready = False
        self.output = None
        self.images = []
        self.boxes = []
        self.faces = []
        self.base_index = 0

    def preprocess(self, base_index, path):
        """
        Aligns images
        Populates boxes with a list of bounding boxes for each image
        Populates faces with a list of cropped faces for each image
        Saves aligned images to path
        :param base_index:
        :param path: save aligned images here
        :return: boxes: bounding boxes for faces [[(x,y,w,h),...],...]
        """
        if len(self.images) < MIN_IMAGES:
            return None
        self.base_index = base_index
        register(self.images, self.base_index)
        for image in self.images:
            f, b = get_faces(image)
            # print('found ' + str(len(f)) + ' faces')
            self.faces.append(f)
            self.boxes.append(b)
        for i in range(len(self.images)):
            cv2.imwrite(path + str(i) + '.jpg', self.images[i])
        self.ready = True
        return self.boxes

    def process(self, indexes, path):
        """
        Performs merge, saves result to path
        :param indexes:  one index for each face, i.e from which image to take
        :param path: where to save result, including terminal
        :return: True iff success
        """
        if not self.ready:
            return None
        num_faces = len(self.faces[self.base_index])
        if len(indexes) != num_faces:
            return None
        base = self.images[self.base_index]
        input_crops, anchors = [], []
        for i in range(num_faces):
            if indexes[i] == self.base_index:
                continue
            input_crops.append(self.faces[indexes[i]][i])
            anchors.append(self.boxes[self.base_index][i])
        self.output = merge(base, input_crops, anchors)
        cv2.imwrite(path, self.output)
        return True
