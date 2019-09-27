import image_processor as ip
import cv2

im1 = cv2.imread(r"test2\0.jpg")
im2 = cv2.imread(r"test2\1.jpg")
im3 = cv2.imread(r"test2\2.jpg")
im4 = cv2.imread(r"test2\3.jpg")

imutils = ip.Imutils()

imutils.add_image(im1)
imutils.add_image(im2)
imutils.add_image(im3)
imutils.add_image(im4)

imutils.preprocess(1)

cv2.imwrite("o0.jpg", imutils.images[0])
cv2.imwrite("o1.jpg", imutils.images[1])
cv2.imwrite("o2.jpg", imutils.images[2])
cv2.imwrite("o3.jpg", imutils.images[3])
