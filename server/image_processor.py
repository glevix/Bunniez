import cv2
import numpy as np

MAX_IMAGES = 4
MIN_IMAGES = 2

cascade_path = r'haarcascades\haarcascade_profileface.xml'
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
        minNeighbors=5,
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


def merge(image, crops, anchors):
    return image


class Imutils:
    ready = False
    output = None
    images = []
    boxes = []
    faces = []
    base_index = 0

    def add_image(self, image):
        if len(self.images) >= MAX_IMAGES:
            raise Exception('Exceeded max images')
        self.images.append(image)
        self.ready = False

    def reset(self):
        self.images = []
        self.boxes = []
        self.faces = []
        self.output = None
        self.ready = False

    def preprocess(self, base_index):
        """
        Aligns images
        Populates boxes with a list of bounding boxes for each image
        Populates faces with a list of cropped faces for each image

        :param base_index:
        :return:
        """
        if len(self.images) < MIN_IMAGES:
            raise Exception('Not enough images to preprocess')
        self.base_index = base_index
        register(self.images, base_index)
        for image in self.images:
            f, b = get_faces(image)
            # print('found ' + str(len(f)) + ' faces')
            self.faces.append(f)
            self.boxes.append(b)
        self.ready = True

    def process(self, indexes):
        if not self.ready:
            raise Exception('Preprocess must be called first')
        num_faces = len(self.faces[self.base_index])
        if len(indexes) != num_faces:
            raise Exception('Wrong number of indexes')
        base = self.images[self.base_index]
        input_crops, anchors = [], []
        for i in range(num_faces):
            if indexes[i] == self.base_index:
                continue
            input_crops.append(self.faces[indexes[i]][i])
            anchors.append(self.boxes[self.base_index][i])
        self.output = merge(base, input_crops, anchors)
