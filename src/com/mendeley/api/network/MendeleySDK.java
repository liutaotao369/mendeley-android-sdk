package com.mendeley.api.network;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;

import com.mendeley.api.exceptions.InterfaceNotImplementedException;
import com.mendeley.api.model.Document;
import com.mendeley.api.model.Folder;
import com.mendeley.api.network.components.DocumentRequestParameters;
import com.mendeley.api.network.components.FileRequestParameters;
import com.mendeley.api.network.components.FolderRequestParameters;
import com.mendeley.api.network.components.Page;
import com.mendeley.api.network.interfaces.AuthenticationInterface;
import com.mendeley.api.network.interfaces.MendeleyDocumentInterface;
import com.mendeley.api.network.interfaces.MendeleyFileInterface;
import com.mendeley.api.network.interfaces.MendeleyFolderInterface;
import com.mendeley.api.network.interfaces.MendeleyInterface;
import com.mendeley.api.network.interfaces.MendeleyProfileInterface;
import com.mendeley.api.network.interfaces.MendeleySignInInterface;
import com.mendeley.api.util.Utils;

/**
 * This class should be instantiated with the calling activity context.
 * The class provides public methods for network calls which are forwarded to the relevant network providers.
 * It also calls the AuthenticationManager for retrieving a valid access token and store the credentials. 
 * The context is used for displaying the authentication WebView, storing credentials in SharedPreferences 
 * and is also checked to see which interfaces the activity implements for sending callbacks once a network task has finished.
 */
public class MendeleySDK {
	protected AuthenticationManager authenticationManager;
	protected MethodtoInvoke methodToInvoke;
	
	protected DocumentNetworkProvider documentNetworkProvider;
	protected FileNetworkProvider fileNetworkProvider;
	protected ProfileNetworkProvider profileNetworkProvider;
	protected FolderNetworkProvider folderNetworkProvider;
	
	protected MendeleyDocumentInterface documentInterface;
	protected MendeleyFolderInterface folderInterface;
	protected MendeleyFileInterface fileInterface;
	protected MendeleyProfileInterface profileInterface;
	MendeleySignInInterface mendeleySignInInterface;

    /**
     * Obtain a handle to the SDK.
     *
     * @param context used for creating the sign-in activity
     * @param callbacks object which implements one or more Mendeley*Interfaces
     * @param clientCredentials your app's Mendeley ID/secret/Uri, from the registration process
     */
	public MendeleySDK(Context context, Object callbacks, ClientCredentials clientCredentials)  {
        initWithWebSignIn(context, callbacks, clientCredentials);
	}

    /**
     * Obtain a handle to the SDK.
     *
     * @param context used for creating the sign-in activity
     * @param callbacks object which implements one or more Mendeley*Interfaces
     * @param clientCredentials your app's Mendeley ID/secret/Uri, from the registration process
     * @param signInCallback used to receive sign in/out events.
     */
	public MendeleySDK(Context context, Object callbacks, MendeleySignInInterface signInCallback,
                       ClientCredentials clientCredentials) {
        this.mendeleySignInInterface = signInCallback;
        initWithWebSignIn(context, callbacks, clientCredentials);
    }

    /**
     * Obtain a handle to the SDK (internal use only).
     * <p>
     * Developer applications should not use this constructor, instead they should pass a context.
     * This constructor is intended for unit testing the SDK.
     */
    public MendeleySDK(Object callbacks, ClientCredentials clientCredentials, UserCredentials userCredentials)  {
        initWithPasswordSignIn(userCredentials, callbacks, clientCredentials);
    }

    /**
     * Obtain a handle to the SDK (internal use only).
     * <p>
     * Developer applications should not use this constructor, instead they should pass a context.
     * This constructor is intended for unit testing the SDK.
     */
    public MendeleySDK(Object callbacks, MendeleySignInInterface signInCallback,
                       ClientCredentials clientCredentials, UserCredentials userCredentials)  {
        this.mendeleySignInInterface = signInCallback;
        initWithPasswordSignIn(userCredentials, callbacks, clientCredentials);
    }

    private void initWithWebSignIn(Context context, Object callbacks, ClientCredentials clientCredentials) {
        AuthenticationManager.configure(
                context,
                createAuthenticationInterface(),
                clientCredentials.clientId,
                clientCredentials.clientSecret,
                clientCredentials.redirectUri);
        init(callbacks);
    }

    private void initWithPasswordSignIn(UserCredentials userCredentials, Object callbacks, ClientCredentials clientCredentials) {
        AuthenticationManager.configure(
                userCredentials.username,
                userCredentials.password,
                createAuthenticationInterface(),
                clientCredentials.clientId,
                clientCredentials.clientSecret,
                clientCredentials.redirectUri);
        init(callbacks);
    }

    private void init(Object callbacks) {
        authenticationManager = AuthenticationManager.getInstance();
        initialiseInterfaces(callbacks);
        if (!hasCredentials()) {
            authenticationManager.authenticate();
        }
    }

    private AuthenticationInterface createAuthenticationInterface() {
        return new AuthenticationInterface() {
            @Override
            public void onAuthenticated() {
                if (mendeleySignInInterface != null) {
                    mendeleySignInInterface.isSignedIn(true);
                }
                invokeMethod();
            }

            @Override
            public void onAuthenticationFail() {
                if (mendeleySignInInterface != null) {
                    mendeleySignInInterface.isSignedIn(false);
                }
                authenticationManager.authenticate();
            }
        };
    }

    /**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param parameters holds optional query parameters, will be ignored if null
	 * @throws InterfaceNotImplementedException
	 */
	public void getFolders(FolderRequestParameters parameters) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{FolderRequestParameters.class}, 
			 				 new Object[]{parameters})) {
			folderNetworkProvider.doGetFolders(parameters);
		}
	}

    /**
     * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
     */
    public void getFolders() throws InterfaceNotImplementedException {
        getFolders((FolderRequestParameters) null);
    }

    /**
     * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
     *
     * @param next reference to next page
     * @throws InterfaceNotImplementedException
     */
    public void getFolders(Page next) throws InterfaceNotImplementedException {
        if (checkNetworkCall(folderInterface,
                new Class[]{FolderRequestParameters.class},
                new Object[]{next})) {
            folderNetworkProvider.doGetFolders(next);
        }
    }

    /**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId id of the folder to get
	 * @throws InterfaceNotImplementedException
	 */
	public void getFolder(String folderId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class}, 
			 				 new Object[]{folderId})) {
			folderNetworkProvider.doGetFolder(folderId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId id of the folder for which to get the document ids
	 */
	public void getFolderDocumentIds(String folderId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class}, 
			 				 new Object[]{folderId})) {
			folderNetworkProvider.doGetFolderDocumentIds(folderId);
		}
	}

    /**
     * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
     *
     * @param next reference to next results page
     */
    public void getFolderDocumentIds(Page next) throws InterfaceNotImplementedException {
        if (checkNetworkCall(folderInterface,
                new Class[]{String.class},
                new Object[]{next})) {
            folderNetworkProvider.doGetFolderDocumentIds(next);
        }
    }

    /**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folder the folder object to post
	 * @throws InterfaceNotImplementedException
	 */
	public void postFolder(Folder folder) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{Folder.class}, 
			 				 new Object[]{folder})) {
			folderNetworkProvider.doPostFolder(folder);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId the id of the folder
	 * @param documentId the id of the document to add to the folder
	 * @throws InterfaceNotImplementedException
	 */
	public void postDocumentToFolder(String folderId, String documentId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class,String.class}, 
			 				 new Object[]{folderId, documentId})) {
			folderNetworkProvider.doPostDocumentToFolder(folderId, documentId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId the id of the folder to delete
	 * @throws InterfaceNotImplementedException
	 */
	public void deleteFolder(String folderId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class}, 
			 				 new Object[]{folderId})) {
			folderNetworkProvider.doDeleteFolder(folderId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId the id of the folder
	 * @param documentId the id of the document to delete
	 * @throws InterfaceNotImplementedException
	 */
	public void deleteDocumentFromFolder(String folderId, String documentId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class, String.class}, 
			 				 new Object[]{folderId, documentId})) {
			folderNetworkProvider.doDeleteDocumentFromFolder(folderId, documentId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the FolderNetworkProvider.
	 * 
	 * @param folderId the id of the folder to patch
	 * @param folder the folder object that holds the new name and parentId data
	 * @throws InterfaceNotImplementedException
	 */
	public void patchFolder(String folderId, Folder folder) throws InterfaceNotImplementedException {
		if (checkNetworkCall(folderInterface,
			 				 new Class[]{String.class, Folder.class}, 
			 				 new Object[]{folderId, folder})) {
			folderNetworkProvider.doPatchFolder(folderId, folder);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the ProfileNetworkProvider.
	 * 
	 * @throws InterfaceNotImplementedException
	 */
	public void getMyProfile() throws InterfaceNotImplementedException {
		if (checkNetworkCall(profileInterface,
			 				 null, null)) {
			profileNetworkProvider.doGetMyProfile();
		}
	}

	
	/**
	 * Checking if call can be executed and forwarding it to the ProfileNetworkProvider.
	 * 
	 * @throws InterfaceNotImplementedException
	 */
	public void getProfile(String profileId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(profileInterface,
			 				 new Class[]{String.class}, 
			 				 new Object[]{profileId})) {
			profileNetworkProvider.doGetProfile(profileId);
		}
	}

    /**
     *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
     *
     * @param parameters holds optional query parameters, will be ignored if null
     */
    public void getFiles(FileRequestParameters parameters) throws InterfaceNotImplementedException {
        if (checkNetworkCall(fileInterface,
                new Class[]{FileRequestParameters.class},
                new Object[]{parameters})) {
            fileNetworkProvider.doGetFiles(parameters);
        }
    }

    /**
     *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
     */
    public void getFiles() throws InterfaceNotImplementedException {
        getFiles((FileRequestParameters) null);
    }

    /**
     *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
     *
     * @param next returned from previous getFiles() call
     */
    public void getFiles(Page next) throws InterfaceNotImplementedException {
        if (checkNetworkCall(fileInterface,
                new Class[]{FileRequestParameters.class},
                new Object[]{next})) {
            fileNetworkProvider.doGetFiles(next);
        }
    }


    /**
	 *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
	 *  
	 * @param fileId the id of the file to get
	 * @param folderPath the path in which to save the file
	 * @throws InterfaceNotImplementedException
	 */
	public void getFile(String fileId, String documentId, String folderPath) throws InterfaceNotImplementedException {
		if (checkNetworkCall(fileInterface,
			 				 new Class[]{String.class, String.class, String.class}, 
		 				 	 new Object[]{fileId, documentId, folderPath})) {
			fileNetworkProvider.doGetFile(fileId, documentId, folderPath);
		}
	}
	
    /**
	 *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
	 *  
	 * @param fileId the id of the file
	 */
	public void cancelDownload(String fileId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(fileInterface,
			 				 new Class[]{String.class}, 
		 				 	 new Object[]{fileId})) {
			fileNetworkProvider.cancelDownload(fileId);
		}
	}
	
    /**
	 *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
	 */
	public void cancelGetFiles() throws InterfaceNotImplementedException {
		if (checkNetworkCall(fileInterface,
			 				 null, 
		 				 	 null)) {
			fileNetworkProvider.cancelGetFiles();
		}
	}
	
	
    /**
	 *  Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 */
	public void cancelGetDocuments() throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
			 				 null, 
		 				 	 null)) {
			documentNetworkProvider.cancelGetDocuments();
		}
	}
	
    /**
	 *  Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 */
	public void cancelGetFolders() throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
			 				 null, 
		 				 	 null)) {
			folderNetworkProvider.cancelGetFolders();
		}
	}
	
    /**
	 *  Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 */
	public void cancelGetDocumentTypes() throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
			 				 null, 
		 				 	 null)) {
			documentNetworkProvider.cancelGetDocumentTypes();
		}
	}
	
	/**
	 *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
	 *  
	 * @throws InterfaceNotImplementedException
	 */
	public void deleteFile(String fileId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(fileInterface,
			 				 new Class[]{String.class}, 
		 				 	 new Object[]{fileId})) {
			fileNetworkProvider.doDeleteFile(fileId);
		}
	}
	
	/**
	 *  Checking if call can be executed and forwarding it to the FileNetworkProvider.
	 *  
	 * @param contentType the content type of the file
	 * @param documentId the id of the document the file is related to
	 * @param filePath the absolute file path
	 * @throws InterfaceNotImplementedException
	 */
	public void postFile(String contentType, String documentId, String filePath) throws InterfaceNotImplementedException {
		if (checkNetworkCall(fileInterface,
			 				 new Class[]{String.class, String.class, String.class}, 
		 				 	 new Object[]{contentType, documentId, filePath})) {
			fileNetworkProvider.doPostFile(contentType, documentId, filePath);
		}
	}
	
	/**
	 * Retrieve a list of documents in the user's library.
	 * 
	 * @param parameters holds optional query parameters, will be ignored if null
	 */
	public void getDocuments(DocumentRequestParameters parameters) throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 new Class[]{DocumentRequestParameters.class}, 
									 new Object[]{parameters})) {
			documentNetworkProvider.doGetDocuments(parameters);
		}
	}

    /**
     * Retrieve a list of documents in the user's library.
     */
    public void getDocuments() throws InterfaceNotImplementedException {
        getDocuments((DocumentRequestParameters) null);
    }
	
	/**
	 * Retrieve subsequent pages of documents in the user's library.
	 * 
	 * @next reference to next page returned by a previous onDocumentsReceived() callback.
	 */
	public void getDocuments(Page next) throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 new Class[]{DocumentRequestParameters.class}, 
									 new Object[]{next})) {
			documentNetworkProvider.doGetDocuments(next);
		}
	}
	
	/**
	 *  Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @throws InterfaceNotImplementedException
	 */
	public void getDocumentTypes() throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 null, 
								 	 null)) {
			documentNetworkProvider.doGetDocumentTypes();
		}
	}


	/**
	 * Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @param documentId the document id to get
	 * @param parameters holds optional query parameters, will be ignored if null
	 * @throws InterfaceNotImplementedException
	 */
	public void getDocument(String documentId, DocumentRequestParameters parameters) throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 new Class[]{String.class,DocumentRequestParameters.class}, 
									 new Object[]{documentId, parameters})) {
			documentNetworkProvider.doGetDocument(documentId, parameters);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @param documentId the document id to be trashed
	 * @throws InterfaceNotImplementedException
	 */
	public void trashDocument(String documentId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 new Class[]{String.class}, 
							 		 new Object[]{documentId})) {
			documentNetworkProvider.doPostTrashDocument(documentId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @param documentId the document id to be deleted
	 * @throws InterfaceNotImplementedException
	 */
	public void deleteDocument(String documentId) throws InterfaceNotImplementedException {
		if (checkNetworkCall(documentInterface,
									 new Class[]{String.class},
									 new Object[]{documentId})) {
			documentNetworkProvider.doDeleteDocument(documentId);
		}
	}
	
	/**
	 * Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @param document the document object to be posted
	 * @throws InterfaceNotImplementedException
	 */
	public void postDocument(Document document) throws InterfaceNotImplementedException {		
		if (checkNetworkCall(documentInterface,
									 new Class[]{Document.class}, 
									 new Object[]{document})) {
			documentNetworkProvider.doPostDocument(document);
		}
	}

	
	/**
	 * Checking if call can be executed and forwarding it to the DocumentNetworkProvider.
	 * 
	 * @param documentId the id of the document to be patched
	 * @param date for the api condition if unmodified since.
	 * @param document the document object
	 * @throws InterfaceNotImplementedException
	 */
	public void patchDocument(String documentId, Date date, Document document) throws InterfaceNotImplementedException {		
		if (checkNetworkCall(documentInterface,
									 new Class[]{String.class, Date.class, Document.class}, 
									 new Object[]{documentId, date, document})) {
			documentNetworkProvider.doPatchDocument(documentId, date, document);
		}
	}
	
	/**
	 * public method to call clearCredentials method on the protected AuthenticationManager
	 */
	public void clearCredentials() {
		authenticationManager.clearCredentials();
	}
	
	/**
	 * Call checkCredentialsAndCopyToNetworkProvider method on the protected AuthenticationManager
	 * 
	 * @return true if credentials are stored already in SharedPreferences.
	 */
	private boolean hasCredentials() {
		return authenticationManager.checkCredentialsAndCopyToNetworkProvider();
	}
	
	/**
	 * Checking the given context to see which interfaces are implemented by the calling activity
	 * and initialising the relevant objects for sending callbacks.
	 * 
	 * @param callbacks the requester's callbacks.
	 */
	private void initialiseInterfaces(Object callbacks) {
		
		if (callbacks instanceof MendeleyDocumentInterface) {
			documentInterface = (MendeleyDocumentInterface) callbacks;
		}
		
		if (callbacks instanceof MendeleyFolderInterface) {
			folderInterface = (MendeleyFolderInterface) callbacks;
		}
		
		if (callbacks instanceof MendeleyFileInterface) {
			fileInterface = (MendeleyFileInterface) callbacks;
		}
		
		if (callbacks instanceof MendeleyProfileInterface) {
			profileInterface = (MendeleyProfileInterface) callbacks;
		}
		
		documentNetworkProvider = new DocumentNetworkProvider(documentInterface);
		fileNetworkProvider = new FileNetworkProvider(fileInterface);
		profileNetworkProvider = new ProfileNetworkProvider(profileInterface);
		folderNetworkProvider = new FolderNetworkProvider(folderInterface);
	} 
		
	/**
	 * First checking that the MendeleyInterface has been instantiated for sending callbacks to the application, if not throwing InterfaceNotImplementedException.
	 * Then checking if client is authenticated, if false initialising the MethodToInvoke object with the calling method name and its arguments
	 * and calling authenticate on the AuthenticationManager, else returns true.
	 * 
	 * @param mendeleyInterface the instance of MendeleyInterface that will be used for the callbacks
	 * @param classes of the arguments of the calling method
	 * @param values of the arguments of the calling method
	 * @return true if network call can be executed
	 * @throws InterfaceNotImplementedException
	 */
	private boolean checkNetworkCall(MendeleyInterface mendeleyInterface, @SuppressWarnings("rawtypes") Class[] classes, Object[] values) throws InterfaceNotImplementedException {
		if (mendeleyInterface == null) {
			throw new InterfaceNotImplementedException("The required MendeleyInterface is not implemented by the calling class");
		}
		if (!authenticationManager.isAuthenticated()) {
			if (classes == null) {
				methodToInvoke = new MethodtoInvoke(new Exception().getStackTrace()[1].getMethodName());
				authenticationManager.authenticate();
			} else {
				methodToInvoke = new MethodtoInvoke(new Exception().getStackTrace()[1].getMethodName(), classes, values);
				authenticationManager.authenticate();
			}
			return false;
		}
		return true;
	}

	/**
	 * Invoking the method stored in the MethodToInvoke object.
	 */
	private void invokeMethod() {
		
		if (methodToInvoke != null) {
			try {
				Method method = null;
				String className = this.getClass().getName();			
				Class<?> clazz = Class.forName(className);  	
				if (methodToInvoke.argumentTypes != null) {
					method = clazz.getMethod(methodToInvoke.methodName, methodToInvoke.argumentTypes);  	
					method.invoke(this, methodToInvoke.arguments);  
				} else {
					method = clazz.getMethod(methodToInvoke.methodName);
					method.invoke(this);
				}
			}
			catch (Exception e) {
				Log.e("", "", e);
			}
		}
	}

	/**
	 * Inner class that holds details of the method and arguments to be called once the client has been authenticated.
	 *
	 */
	protected class MethodtoInvoke {
		
		String methodName;
		@SuppressWarnings("rawtypes")
		Class[] argumentTypes;
		Object[] arguments;
		
		MethodtoInvoke(String methodName) {
			this.methodName = methodName;
			argumentTypes = null;
			arguments = null;
		}
		
		MethodtoInvoke(String methodName, @SuppressWarnings("rawtypes") Class[] argumentTypes, Object[] arguments) {
			this.methodName = methodName;
			this.argumentTypes = argumentTypes;
			this.arguments = arguments;
		}
	}
	
	/**
	 * public default constructor for junit testing.
	 */
	public MendeleySDK() {
	}
}
