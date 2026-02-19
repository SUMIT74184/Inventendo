FROM ubuntu:latest
LABEL authors="sumit"

ENTRYPOINT ["top", "-b"]