package com.dpillay.tools.tail4j.model;

public class TailContents<T> {
	private T contents = null;
	private long size = 0;
	private boolean newLine = false;

	public boolean hasNewLine() {
		return newLine;
	}

	public void setNewLine(boolean newLine) {
		this.newLine = newLine;
	}

	public T getContents() {
		return contents;
	}

	public long getSize() {
		return size;
	}

	public TailContents(T contents, long size, boolean newLine) {
		super();
		this.contents = contents;
		this.size = size;
		this.newLine = newLine;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TailContents<?> other = TailContents.class.cast(obj);
		if (contents == null) {
			if (other.contents != null)
				return false;
		} else if (!contents.equals(other.contents))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TailContents [contents=" + contents + ", size=" + size + "]";
	}
}
