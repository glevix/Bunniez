import image_processor as ip
import cv2

im1 = cv2.imread(r"test3\0.jpeg")
im2 = cv2.imread(r"test3\1.jpeg")
im3 = cv2.imread(r"test3\2.jpeg")

imutils = ip.Imutils()

imutils.add_image(im1)
imutils.add_image(im2)
imutils.add_image(im3)

imutils.preprocess(0)
imutils.process([1, 1])

cv2.imwrite("output.jpg", imutils.output)
