package com.dpillay.tools.tail4j.model;

public class TailEvent<T> {
	private TailContents<T> tailContents = null;

	public TailEvent(TailContents<T> tailContents) {
		super();
		this.tailContents = tailContents;
	}

	public TailContents<T> getTailContents() {
		return tailContents;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tailContents == null) ? 0 : tailContents.hashCode());
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
		TailEvent<?> other = TailEvent.class.cast(obj);
		if (tailContents == null) {
			if (other.tailContents != null)
				return false;
		} else if (!tailContents.equals(other.tailContents))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TailEvent [tailContents=" + tailContents + "]";
	}

	public static <T> TailEvent<T> generateEvent(T line, long size, boolean newLine) {
		TailContents<T> tailContents = new TailContents<T>(line, size, newLine);
		TailEvent<T> tailEvent = new TailEvent<T>(tailContents);
		return tailEvent;
	}
}
