FROM postgres:17-alpine

RUN apk add --no-cache bash coreutils findutils restic rclone tzdata

COPY backup.sh restore.sh /opt/backup/
RUN chmod 0755 /opt/backup/backup.sh /opt/backup/restore.sh

WORKDIR /opt/backup
