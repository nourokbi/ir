class RankedFile {
    private int fileId;
    private double cosineSimilarity;

    public RankedFile(int fileId, double cosineSimilarity) {
        this.fileId = fileId;
        this.cosineSimilarity = cosineSimilarity;
    }

    public int getFileId() {
        return fileId;
    }

    public double getCosineSimilarity() {
        return cosineSimilarity;
    }
}