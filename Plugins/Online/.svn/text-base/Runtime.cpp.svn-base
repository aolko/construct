// Runtime.cpp - Runtime implementation of object
//

// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Runtime functions
//////////////////////////////////////////////////////////////////////////////////
#ifdef RUN_ONLY

// ExtObject constructor:
// Only use for class initializer list.  Object initialisation must be done in OnCreate.
// It is not safe to make runtime calls here: do so in OnCreate.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
: renderer(pVRuntime->pRenderer)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
}

// This is called just after the constructor when your object has been created.  Construct has set
// up your object here so it is safe to make runtime calls.
void ExtObject::OnCreate()
{
	// Load the edittime data that was serialized.
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	// Read the data.  Same format as you exported in EditExt::Serialize.
	// Your runtime loader must be able to load all versions!
	int Version = 0;
	ar >> Version;
	//ar >> myValue;

	// Finished reading data
	ar.detach();

	// Set default dimensions
	info.x = info.editObject->eX;
	info.y = info.editObject->eY;
	info.w = info.editObject->eWidth;
	info.h = info.editObject->eHeight;
	info.angle = 0.0f;

	uniquePlayerID = 0;
	server = false;

	player = 0;

	// Update bounding box
	pRuntime->UpdateBoundingBox(this);


	raknet = RakNet::RakPeerInterface::GetInstance();
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
	raknet->Shutdown(500, 0);
}

// Called every frame, before the events and after drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
BOOL ExtObject::OnFrame()
{
	processPackets();
	return 0;	// Do not call again
}

// Called every frame, after the events and before drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
// It is not safe to destroy objects in OnFrame2().  If you have to do this, use OnFrame().
// If you are storing any pointers to CRunObjects, check the info.destroying flag here.  If it is true,
// you must reset the pointer to NULL as it is no longer valid.
BOOL ExtObject::OnFrame2()
{
	return 1;	// Do not call again
}

// WindowProc:  called when a window message, or WM_COMMAND specifying your window,
// is sent to Construct.  You must call RegisterWindow() before Construct will send
// you messages for the given window.
LRESULT ExtObject::WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
	return DefWindowProc(hWnd, uMessage, wParam, lParam);
}

BOOL ExtObject::PreTranslateMessage(MSG* msg)
{
	return 0;
}

// For global objects
void ExtObject::OnFrameChange(int oldFrame, int newFrame)
{
	// Do anything your global object needs when the frame changes
	// oldFrame is -1 on start of app, newFrame is -1 on end of app
}

// Player defined functions
long ExtObject::GetData(int id, void* param)
{
	return 0;
}

long ExtObject::CallFunction(int id, void* param)
{
	return 0;
}

RakNet::MessageID GetPacketIdentifier(RakNet::Packet *p)
{
	if (p==0)
		return 255;

	if ((RakNet::MessageID)p->data[0] == ID_TIMESTAMP)
	{
		RakAssert(p->length > sizeof(RakNet::MessageID) + sizeof(RakNet::Time));
		return (RakNet::MessageID) p->data[sizeof(RakNet::MessageID) + sizeof(RakNet::Time)];
	}
	else
		return (RakNet::MessageID) p->data[0];
}
void print_debug(string message)
{
	//MessageBox(0, message.c_str(), 0, 0);
}

void TimeStamp(RakNet::BitStream& stream, RakNet::MessageID messageID)
{
	stream.Write( (RakNet::MessageID) ID_TIMESTAMP);
	stream.Write( (RakNet::Time) RakNet::GetTime());
	stream.Write( messageID );
}

void WriteExpStore( RakNet::BitStream &stream, ExpStore &s ) 
{
	stream.Write(s.Type()); 

	switch(s.Type())
	{
	case EXPTYPE_INTEGER:
		stream.Write(s.GetInt());
		break;
	case EXPTYPE_FLOAT:
		stream.Write(s.GetFloat());
		break;
	case EXPTYPE_STRING:
		RakNet::RakString string;
		string = s.GetString();
		stream.Write( string );
		break;
	}
}

void ReadExpStore( RakNet::BitStream &stream, ExpStore& s ) 
{
	RakNet::RakString read_string;
	int read_int;
	float read_float;
	ExpType type;
	stream.Read( type );

	switch(type)
	{
	case EXPTYPE_INTEGER:
		stream.Read(read_int);
		s = read_int;
		break;
	case EXPTYPE_FLOAT:
		stream.Read(read_float);
		s = read_float;
		break;
	case EXPTYPE_STRING:
		stream.Read( read_string );
		s = read_string;
		break;
	}	
}

void ExtObject::processPackets()
{
	for(packet = raknet->Receive(); packet; packet = raknet->Receive())
	{
		remotePlayerID.push_back( packet->guid );
		RakNet::MessageID packetIdentifier = GetPacketIdentifier(packet);
		switch (packetIdentifier)
		{
		case ID_DISCONNECTION_NOTIFICATION:
			if(server)
			{
				PlayerMap::iterator p = players.map.find( packet->guid );
				if( p!= players.map.end() )
				{
					Player& leftPlayer = *p->second;
					remotePlayerID.push_back( leftPlayer.guid );
					pRuntime->GenerateEvent(info.oid, condition::cOnPlayerDisconnect, this);
					remotePlayerID.pop_back();

					// A client has disconnected from us. We must send everyone a notification about them
					RakNet::BitStream stream;
					TimeStamp(stream, ID_CONSTRUCT_PLAYER_LEFT );
					stream << leftPlayer.guid;

					raknet->Send(&stream, HIGH_PRIORITY, RELIABLE_ORDERED, 0, RakNet::UNASSIGNED_SYSTEM_ADDRESS, true);

					players.list.remove(leftPlayer);
					players.map.erase(p);
					
				}
			}
			//else
			{
				// We have been disconnected from the server :(
				pRuntime->GenerateEvent(info.oid, condition::cOnDisconnect, this);
				isConnected = false;
			}
			break;
		case ID_CONNECTION_LOST:
			pRuntime->GenerateEvent(info.oid, condition::cOnConnectionLost, this);
			isConnected=false;
			break;
		case ID_CONNECTION_BANNED:
			pRuntime->GenerateEvent(info.oid, condition::cOnBanned, this);
			isConnected=false;
			break;
		case ID_CONNECTION_REQUEST_ACCEPTED:
		{
			// We have connected, ask for joining info
			isConnected=true;

			RakNet::BitStream stream;
			TimeStamp(stream, ID_CONSTRUCT_ASK_FOR_JOINING_INFO);

			raknet->Send(&stream, HIGH_PRIORITY, RELIABLE_ORDERED, 0, RakNet::UNASSIGNED_SYSTEM_ADDRESS, true);
			
			break;
		}
		case ID_CONSTRUCT_ASK_FOR_JOINING_INFO:
		{
			if(server)
			{		
				int newPlayerID = getUniquePlayerID();
				Player newPlayer(packet->guid, newPlayerID);

				// We have a message from a newly connected client asking us for info
				// Send a message back to that client
				{
	

					RakNet::BitStream stream;
					TimeStamp(stream, ID_CONSTRUCT_JOINING_INFO);

					stream.Write(newPlayerID);
					stream.Write((int) players.size());
					for ( PlayerList::iterator p = players.list.begin(); p!= players.list.end(); p++ )
						p->Serialize(true, stream);

					raknet->Send(&stream, HIGH_PRIORITY, RELIABLE_ORDERED, 0, packet->guid, false);
				}


				players.push_back(packet->guid, newPlayer);

				remotePlayerID.push_back( newPlayer.guid );
				pRuntime->GenerateEvent(info.oid, condition::cOnPlayerConnect, this);
				remotePlayerID.pop_back();
			
				// Now send a message to every other client telling them a player has joined
				{
					RakNet::BitStream stream;
					TimeStamp(stream, ID_CONSTRUCT_PLAYER_JOINED);

					newPlayer.Serialize(true, stream);

					raknet->Send(&stream, HIGH_PRIORITY, RELIABLE_ORDERED, 0, packet->guid, true);
				}
			}

			break;
		}
		case ID_CONSTRUCT_JOINING_INFO:
			{
				// Client: Received a message saying a player has joined
				RakNet::BitStream stream(packet->data, packet->length, false);

				RakNet::MessageID useTimeStamp; 
				RakNet::Time timeStamp; 
				RakNet::MessageID typeId; 

				stream.Read(useTimeStamp);
				stream.Read(timeStamp);
				stream.Read(typeId);

				int playerID;
				stream.Read(playerID);

				int numberOfPlayers = 0;
				stream.Read(numberOfPlayers);

				RakNet::RakNetGUID gui = raknet->GetGuidFromSystemAddress(RakNet::UNASSIGNED_SYSTEM_ADDRESS);

				players.push_back(gui, Player(gui, playerID) );
				player = &players.list.back();

				pRuntime->GenerateEvent(info.oid, condition::cOnConnect, this);

				for( int i = 0; i < numberOfPlayers; i++)
				{
					Player player = Player(stream);
					players.push_back(player.guid, player);

					remotePlayerID.push_back(player.guid);
					pRuntime->GenerateEvent(info.oid, condition::cOnPlayerHere, this);
					remotePlayerID.pop_back();
				}


				break;
			}

		case ID_CONSTRUCT_PLAYER_JOINED:
		{
			// We have been told someone has joined the game
			RakNet::BitStream stream(packet->data, packet->length, false);

			RakNet::MessageID useTimeStamp; 
			RakNet::Time timeStamp; 
			RakNet::MessageID typeId; 

			stream.Read(useTimeStamp);
			stream.Read(timeStamp);
			stream.Read(typeId);

			Player newPlayer(stream);
			players.push_back(newPlayer.guid, newPlayer );

			remotePlayerID.push_back( newPlayer.guid );
			pRuntime->GenerateEvent(info.oid, condition::cOnPlayerConnect, this);
			remotePlayerID.pop_back();

			break;
		}

		case ID_CONSTRUCT_PLAYER_LEFT:
		{
			// The server has responded to our call for info
			RakNet::BitStream stream(packet->data, packet->length, false);

			RakNet::MessageID useTimeStamp; 
			RakNet::Time timeStamp; 
			RakNet::MessageID typeId; 
			RakNet::RakNetGUID guid;

			stream.Read(useTimeStamp);
			stream.Read(timeStamp);
			stream.Read(typeId);
			stream.Read(guid);

			Player leftPlayer(guid, 0);
			PlayerMap::iterator p = players.map.find(leftPlayer.guid);
			if(p != players.map.end())
			{
				leftPlayer.id = p->second->id;
				remotePlayerID.push_back( leftPlayer.guid );
				pRuntime->GenerateEvent(info.oid, condition::cOnPlayerDisconnect, this);
				remotePlayerID.pop_back();
				players.map.erase(p);
				players.list.remove(leftPlayer);
			}
			

			break;
		}

	


		case ID_NO_FREE_INCOMING_CONNECTIONS:
			print_debug("Too many peoples :/");
			pRuntime->GenerateEvent(info.oid, condition::cOnServerFull, this);
			isConnected=false;
			break;
		case ID_REMOTE_CONNECTION_LOST:
			print_debug("A player disconnected");
			pRuntime->GenerateEvent(info.oid, condition::cOnPlayerDisconnect, this);
			break;
		case ID_REMOTE_NEW_INCOMING_CONNECTION:
			print_debug("A player joined! yay :D");
			pRuntime->GenerateEvent(info.oid, condition::cOnPlayerConnect, this);
			break;
		case ID_CONNECTION_ATTEMPT_FAILED:
			print_debug("Failed to connect");
			// todo
			isConnected=false;
			break;
		case ID_CONSTRUCT_MESSAGE:
			print_debug("Construct message ahoy!");
	
			RakNet::BitStream stream(packet->data, packet->length, false);

			RakNet::MessageID useTimeStamp; // Assign this to ID_TIMESTAMP
			RakNet::Time timeStamp; // Put the system time in here returned by RakNet::GetTime()
			RakNet::MessageID typeId; // This will be assigned to a type I've added after ID_USER_PACKET_ENUM, lets say ID_SET_TIMED_MINE

			stream.Read(useTimeStamp);
			stream.Read(timeStamp);
			stream.Read(typeId);

			RakNet::RakString read_string;
			stream.Read( read_string );
			message = read_string;

			int numberOfParams;
			stream.Read(numberOfParams);

			messageParamList.clear();
			for(int i = 0; i < numberOfParams; i++)
			{
				ExpStore s;
				ReadExpStore(stream, s);

				messageParamList.push_back(s);
			}
			RakNet::RakNetGUID guid;
			stream.Read(guid);

			remotePlayerID.push_back( guid );

			pRuntime->GenerateEvent(info.oid, condition::cOnMessage, this);
			pRuntime->GenerateEvent(info.oid, condition::cOnAnyMessage, this);

			remotePlayerID.pop_back();
			break;
		}
		raknet->DeallocatePacket(packet);

		remotePlayerID.pop_back();
	}
	packet = NULL;
}


#else //ifdef RUN_ONLY

CRunObject* WINAPI RTCreateObject(bin& ar, VRuntime* pRuntime) 
{
	return NULL;
}

void WINAPI RTDestroyObject(ExtObject* object)
{
}

#endif