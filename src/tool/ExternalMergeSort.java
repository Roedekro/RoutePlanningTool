package tool;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * External Merge Sort for the various classes of the tool.
 * Set M, B and k using the constructor and sort using the specific methods.
 * Using exceptions to detect EOF so capture all exceptions here.
 * @author Martin
 * 
 */
class ExternalMergeSort {

	protected long M = 2000000000; // ~2gb
	protected int B = 4096;
	protected int k = (int) (M/B);
	protected LinkedList<String> files;
	protected int sizeOfIncompleteNode = 24;
	//protected int sizeOfIncompleteEdge = 50;
	protected int sizeOfIncompleteEdge = 60;
	
	/**
	 * Set initial parameters.
	 * @param M Size of Memory.
	 * @param B Block Size.
	 * @param k Number of merges in the k-way merge (M/B).
	 */
	protected ExternalMergeSort(long M, int B, int k) {
		
		if(M > 0) this.M = M;
		if(B > 0) this.B = B;
		if(k > 0) {
			this.k = k;
		}
		else {
			this.k = (int) (this.M/this.B);
		}
	}
	
	/**
	 * Sort IncompleteNodes, nodes without edges.
	 * @param input File containing the elements to be sorted.
	 * @param output Name of desired output file.
	 */
	protected void sortIncompleteNodes(String input, String output) {
		
		// Used to require N, but changed.
		
		long numberOfItems = M/sizeOfIncompleteNode;
		ObjectInputStream oin = null;
		ObjectOutputStream oout = null;
		try {
			oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Comparator<IncompleteNode> comparator = new Comparator<IncompleteNode>() {
			@Override
			public int compare(IncompleteNode node1, IncompleteNode node2) {
				return Long.compare(node1.id, node2.id);
			}
		};
		
		int pass = 0;
		files = new LinkedList<String>();
		
		// Scan through the input file in chunks of numberOfItems we can hold in internal memory
		//while(count < N) {
		boolean read = true;
		while(read) {
			pass++;
			
			//System.out.println("Pass "+pass+" to sort "+numberOfItems+" elements");
			
			try {
				oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			files.add("sort"+pass);
			ArrayList<IncompleteNode> temp = new ArrayList<IncompleteNode>();
			/*if (count > N) {
				numberOfItems = (int) (N%numberOfItems);
			}*/
			
			// Read to internal memory
			for(int i = 0; i < numberOfItems; i++) {
				try {
					temp.add((IncompleteNode) oin.readUnshared());
					//oin.reset();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// Reached end of file
					read = false;
					numberOfItems = i;
				}
			}
			
			if(numberOfItems == 0) {
				files.removeLast();
			}
			
			// Sort in internal memory
			Collections.sort(temp,comparator);
			
			// Output to temporary file
			for(int i = 0; i < numberOfItems; i++) {
				try {
					//System.out.println("Writing: " +temp.get(i).id);
					oout.writeUnshared(temp.get(i));
					//oout.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				oout.flush();
				oout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			oin.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Now that each chunk is sorted and outputted to a file recursively merge these files k at a time
		int numberOfFiles = files.size();
		while(true) {
			
			ArrayList<ObjectInputStream> inputs = new ArrayList<ObjectInputStream>();
			HeapObject[] merge = new HeapObject[k+1];
			
			if(numberOfFiles > k) {
				// Merge k files at a time
				
				pass++;
				numberOfFiles = numberOfFiles-k;
				
				// Populate the list of input streams
				for(int i = 0; i < k; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteNode tempNode = null;
				for(int i = 0; i < k; i++) {
					try {
						tempNode = (IncompleteNode) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempNode.id,i,tempNode);
				}
				
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, k);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				files.add("sort"+pass);
				numberOfFiles++;
				
				// Now merge them
				int numberOfLiveStreams = k;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempNode = (IncompleteNode) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempNode.id,id,tempNode);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			else {
				// Merge final files, clean up and return
				
				// Populate the list of input streams
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteNode tempNode = null;
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						tempNode = (IncompleteNode) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempNode.id,i,tempNode);
				}
				
				/*System.out.println("Merging "+numberOfFiles);
				System.out.println("Merge contains:");
				for(int i = 0; i < numberOfFiles; i++) {
					System.out.println(merge[i+1].val);
				}*/
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, numberOfFiles);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}				
				
				// Now merge them
				int numberOfLiveStreams = numberOfFiles;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempNode = (IncompleteNode) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempNode.id,id,tempNode);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Delete all the files we have used throughout the sorting
				File f;
				for(int i = 1; i <= pass; i++) {
					f= new File("sort"+i);
					try {
						Files.delete(f.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// Done!
				return;
			}
		}

	}
	
	/**
	 * Sort IncompleteEdges by their first node.
	 * @param input File containing the elements to be sorted.
	 * @param output Name of desired output file.
	 */
	protected void sortIncompleteEdgesByNodeID1(String input, String output) {
		
		long numberOfItems = M/sizeOfIncompleteEdge;
		ObjectInputStream oin = null;
		ObjectOutputStream oout = null;
		try {
			oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Comparator<IncompleteEdge> comparator = new Comparator<IncompleteEdge>() {
			@Override
			public int compare(IncompleteEdge edge1, IncompleteEdge edge2) {
				int ret = Long.compare(edge1.nodeID1, edge2.nodeID1);
				if(ret == 0) {
					return Long.compare(edge1.nodeID2, edge2.nodeID2);
				}
				return ret;
			}
		};
		
		int pass = 0;
		files = new LinkedList<String>();
		
		// Scan through the input file in chunks of numberOfItems we can hold in internal memory
		boolean read = true;
		while(read) {
			pass++;
			
			try {
				oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			files.add("sort"+pass);
			ArrayList<IncompleteEdge> temp = new ArrayList<IncompleteEdge>();
			
			// Read to internal memory
			for(int i = 0; i < numberOfItems; i++) {
				try {
					temp.add((IncompleteEdge) oin.readUnshared());
					//oin.reset();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// Reached end of file
					read = false;
					numberOfItems = i;
				}
			}
			
			if(numberOfItems == 0) {
				files.removeLast();
			}
			
			// Sort in internal memory
			Collections.sort(temp,comparator);
			
			// Output to temporary file
			for(int i = 0; i < numberOfItems; i++) {
				try {
					//System.out.println("Writing: " +temp.get(i).id);
					oout.writeUnshared(temp.get(i));
					//oout.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				oout.flush();
				oout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			oin.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Now that each chunk is sorted and outputted to a file recursively merge these files k at a time
		int numberOfFiles = files.size();
		while(true) {
			
			ArrayList<ObjectInputStream> inputs = new ArrayList<ObjectInputStream>();
			HeapObject[] merge = new HeapObject[k+1];
			
			if(numberOfFiles > k) {
				// Merge k files at a time
				
				pass++;
				numberOfFiles = numberOfFiles-k;
				
				// Populate the list of input streams
				for(int i = 0; i < k; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteEdge tempEdge = null;
				for(int i = 0; i < k; i++) {
					try {
						tempEdge = (IncompleteEdge) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempEdge.nodeID1,i,tempEdge);
				}
				
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, k);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				files.add("sort"+pass);
				numberOfFiles++;
				
				// Now merge them
				int numberOfLiveStreams = k;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempEdge = (IncompleteEdge) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempEdge.nodeID1,id,tempEdge);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			else {
				// Merge final files, clean up and return
				
				// Populate the list of input streams
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteEdge tempEdge = null;
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						tempEdge = (IncompleteEdge) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempEdge.nodeID1,i,tempEdge);
				}
				
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, numberOfFiles);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}				
				
				// Now merge them
				int numberOfLiveStreams = numberOfFiles;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempEdge = (IncompleteEdge) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempEdge.nodeID1,id,tempEdge);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Delete all the files we have used throughout the sorting
				File f;
				for(int i = 1; i <= pass; i++) {
					f= new File("sort"+i);
					try {
						Files.delete(f.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// Done!
				return;
			}
		}

	}
	
	/**
	 * Sort IncompleteEdges by their second node.
	 * @param input File containing the elements to be sorted.
	 * @param output Name of desired output file.
	 */
	protected void sortIncompleteEdgesByNodeID2(String input, String output) {
		
		long numberOfItems = M/sizeOfIncompleteEdge;
		ObjectInputStream oin = null;
		ObjectOutputStream oout = null;
		try {
			oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Comparator<IncompleteEdge> comparator = new Comparator<IncompleteEdge>() {
			@Override
			public int compare(IncompleteEdge edge1, IncompleteEdge edge2) {
				int ret = Long.compare(edge1.nodeID2, edge2.nodeID2);
				if(ret == 0) {
					return Long.compare(edge1.nodeID1, edge2.nodeID1);
				}
				return ret;
			}
		};
		
		int pass = 0;
		files = new LinkedList<String>();
		
		// Scan through the input file in chunks of numberOfItems we can hold in internal memory
		boolean read = true;
		while(read) {
			pass++;
			
			try {
				oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			files.add("sort"+pass);
			ArrayList<IncompleteEdge> temp = new ArrayList<IncompleteEdge>();
			
			// Read to internal memory
			for(int i = 0; i < numberOfItems; i++) {
				try {
					temp.add((IncompleteEdge) oin.readUnshared());
					//oin.reset();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// Reached end of file
					read = false;
					numberOfItems = i;
				}
			}
			
			if(numberOfItems == 0) {
				files.removeLast();
			}
			
			// Sort in internal memory
			Collections.sort(temp,comparator);
			
			// Output to temporary file
			for(int i = 0; i < numberOfItems; i++) {
				try {
					//System.out.println("Writing: " +temp.get(i).id);
					oout.writeUnshared(temp.get(i));
					//oout.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				oout.flush();
				oout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			oin.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Now that each chunk is sorted and outputted to a file recursively merge these files k at a time
		int numberOfFiles = files.size();
		while(true) {
			
			ArrayList<ObjectInputStream> inputs = new ArrayList<ObjectInputStream>();
			HeapObject[] merge = new HeapObject[k+1];
			
			if(numberOfFiles > k) {
				// Merge k files at a time
				
				pass++;
				numberOfFiles = numberOfFiles-k;
				
				// Populate the list of input streams
				for(int i = 0; i < k; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteEdge tempEdge = null;
				for(int i = 0; i < k; i++) {
					try {
						tempEdge = (IncompleteEdge) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempEdge.nodeID2,i,tempEdge);
				}
				
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, k);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("sort"+pass),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				files.add("sort"+pass);
				numberOfFiles++;
				
				// Now merge them
				int numberOfLiveStreams = k;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempEdge = (IncompleteEdge) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempEdge.nodeID2,id,tempEdge);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			else {
				// Merge final files, clean up and return
				
				// Populate the list of input streams
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						inputs.add(new ObjectInputStream(new BufferedInputStream(
								new FileInputStream(files.remove()),B)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}				
				}
				
				// Read in a single element from each buffer
				IncompleteEdge tempEdge = null;
				for(int i = 0; i < numberOfFiles; i++) {
					try {
						tempEdge = (IncompleteEdge) inputs.get(i).readUnshared();
						//inputs.get(i).reset();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					merge[i+1] = new HeapObject(tempEdge.nodeID2,i,tempEdge);
				}
				
				// Establish heap order
				Heap heap = new Heap();
				heap.setheap(merge, numberOfFiles);
				
				// Open outputstream
				try {
					oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}				
				
				// Now merge them
				int numberOfLiveStreams = numberOfFiles;
				HeapObject tempObject = null;
				int id = 0;
				while(numberOfLiveStreams > 0) {
					tempObject = heap.outheap(merge, numberOfLiveStreams);
					try {
						oout.writeUnshared(tempObject.object);
						//oout.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
					id = tempObject.id;
					try {
						tempEdge = (IncompleteEdge) inputs.get(id).readUnshared();
						//inputs.get(id).reset();
						// Below wont execute if EOF detected
						tempObject = new HeapObject(tempEdge.nodeID2,id,tempEdge);
						heap.inheap(merge, tempObject, numberOfLiveStreams-1);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// Catch end of file
						numberOfLiveStreams--;
						try {
							inputs.get(id).close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// Cleanup
				try {
					oout.flush();
					oout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Delete all the files we have used throughout the sorting
				File f;
				for(int i = 1; i <= pass; i++) {
					f= new File("sort"+i);
					try {
						Files.delete(f.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// Done!
				return;
			}
		}

	}
	
}
