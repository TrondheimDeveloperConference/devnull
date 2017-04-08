FROM java:8
ADD devnull/ /
ADD wait-for-it.sh /
ENV app.home /root
EXPOSE 8084
CMD /wait-for-it.sh db:5432 -- /root/bin/jetty
