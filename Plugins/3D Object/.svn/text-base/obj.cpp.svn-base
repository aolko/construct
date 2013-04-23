#include "StdAfx.h"

using namespace std;

template<typename T>
T* cross_reference_vectors(std::vector<T>& new_container, const std::vector<T>& old_container, int index)
{
	if(index < 0 || index >= old_container.size())
		return NULL;

	std::vector<T>::iterator i = find(new_container.begin(), new_container.end(), old_container[index] );
	if(i == new_container.end())
		return NULL;
	else
		return &*i;
}

void obj::scale(float scale)
{
	vector<cr::point3d>::iterator iv = v_original.begin();
	for(; iv != v_original.end(); iv++)
		*iv *= scale;

	// Scale bounding box
	for(int i = 0; i < 8; i++)
	bb[i] *= scale;
}

void obj::load_from_file(string path)
{
	//assimp_load_from_file(path);
	/*std::ifstream ifs( path.c_str() );
	std::string str((std::istreambuf_iterator<char>(ifs)), std::istreambuf_iterator<char>());
	std::stringstream ss;
	ss << str;

	load_from_string(ss);*/

}

void obj::calculate_bounding_box()
{
	// Now loop all the vertices and work out the min max for each dimension!

	bb[0] = bb[1] = bb[2] = bb[3] = bb[4] = bb[5] = bb[6] = bb[7] = cr::point3d(0,0,0);

	if(v.size() > 0)
	{
		cr::point3d min_point = v[0];
		cr::point3d max_point = v[0];

		vector<cr::point3d>::iterator iv = v.begin();
		iv ++;
		for(; iv!= v.end(); iv++)
		{
			min_point.x = min(min_point.x, iv->x);
			min_point.y = min(min_point.y, iv->y);
			min_point.z = min(min_point.z, iv->z);

			max_point.x = max(max_point.x, iv->x);
			max_point.y = max(max_point.y, iv->y);
			max_point.z = max(max_point.z, iv->z);
		}

		bb[0] = cr::point3d( min_point.x, min_point.y, min_point.z );
		bb[1] = cr::point3d( min_point.x, min_point.y, max_point.z );
		bb[2] = cr::point3d( min_point.x, max_point.y, min_point.z );
		bb[3] = cr::point3d( min_point.x, max_point.y, max_point.z );
		bb[4] = cr::point3d( max_point.x, min_point.y, min_point.z );
		bb[5] = cr::point3d( max_point.x, min_point.y, max_point.z );
		bb[6] = cr::point3d( max_point.x, max_point.y, min_point.z );
		bb[7] = cr::point3d( max_point.x, max_point.y, max_point.z );

	}
}

void obj::assimp_load_from_mesh(aiMesh& mesh)
{
	objs.push_back(obj_object());
	obj_object& obj = objs.back();

	cr::point3d pos;
	cr::point coord;
	cr::point3d normal;

	v.reserve(mesh.mNumVertices);
	vt.reserve(mesh.mNumVertices);
	vn.reserve(mesh.mNumVertices);
	points.reserve(mesh.mNumVertices);

	for( unsigned i = 0; i < mesh.mNumVertices; i++ )
	{	
		pos.x = mesh.mVertices[i].x;
		pos.y = mesh.mVertices[i].y;
		pos.z = mesh.mVertices[i].z;

		normal.x	= mesh.mNormals[i].x;
		normal.y	= mesh.mNormals[i].y;
		normal.z	= mesh.mNormals[i].z;

		//If the mesh has UV coordinates..
		if( mesh.mNumUVComponents[0] >= 2 )
		{
			coord.x = mesh.mTextureCoords[0][i].x;
			coord.y = 1-mesh.mTextureCoords[0][i].y;
		}
		else
		{
			coord.x = 0; coord.y = 0;
		}

		v.push_back(pos);
		vn.push_back(normal);
		vt.push_back(coord);

		obj_face_point pt;
		pt.v = &v.back();
		pt.vn = &vn.back();
		pt.vt = &vt.back();

		points.push_back(pt);
	}

	//Should only be 3 faces per polygon if assimp did its work
	for( unsigned i = 0; i < mesh.mNumFaces; i++ )
	{
		obj_face face;
		face.indexes.push_back(mesh.mFaces[i].mIndices[0]);
		face.indexes.push_back(mesh.mFaces[i].mIndices[1]);
		face.indexes.push_back(mesh.mFaces[i].mIndices[2]);
		obj.faces.push_back(face);

		number_of_indexes += 3;
	}

	v_original = v;
	vt_original = vt;
	vn_original = vn;

	calculate_bounding_box();
}

void obj_array::calculate_bounding_box()
{
	// Now loop all the vertices and work out the min max for each dimension!
	bb[0] = bb[1] = bb[2] = bb[3] = bb[4] = bb[5] = bb[6] = bb[7] = cr::point3d(0,0,0);
	bool first = true;

	cr::point3d min_point;
	cr::point3d max_point;

	for( vector<obj>::iterator o = objs.begin(); o != objs.end(); o++)
	{
		for(int b = 0; b < 8; b++)
		{
			if(first)
			{
				min_point = o->bb[0];
				max_point = o->bb[0];
				first = false;
			}
			else
			{
				min_point.x = min(min_point.x, o->bb[b].x);
				min_point.y = min(min_point.y, o->bb[b].y);
				min_point.z = min(min_point.z, o->bb[b].z);

				max_point.x = max(max_point.x, o->bb[b].x);
				max_point.y = max(max_point.y, o->bb[b].y);
				max_point.z = max(max_point.z, o->bb[b].z);
			}
		}
	}
	bb[0] = cr::point3d( min_point.x, min_point.y, min_point.z );
	bb[1] = cr::point3d( min_point.x, min_point.y, max_point.z );
	bb[2] = cr::point3d( min_point.x, max_point.y, min_point.z );
	bb[3] = cr::point3d( min_point.x, max_point.y, max_point.z );
	bb[4] = cr::point3d( max_point.x, min_point.y, min_point.z );
	bb[5] = cr::point3d( max_point.x, min_point.y, max_point.z );
	bb[6] = cr::point3d( max_point.x, max_point.y, min_point.z );
	bb[7] = cr::point3d( max_point.x, max_point.y, max_point.z );

}

void obj_array::load_from_file(string path)
{
	Assimp::Importer importer;
	const aiScene* scene = importer.ReadFile( path,  
		aiProcess_Triangulate			|
		aiProcess_GenSmoothNormals		|
		aiProcess_PreTransformVertices  |
		aiProcess_SplitLargeMeshes      |
		aiProcess_OptimizeMeshes        |
		aiProcess_OptimizeGraph
		);


	//If the file loaded
	if( scene == NULL )
		return;

	if( ! scene->HasMeshes() )
		return;

	objs.reserve(scene->mNumMeshes);
	for(int i = 0; i < scene->mNumMeshes;i++)
	{
		aiMesh& mesh = *(scene->mMeshes[i]);
		objs.push_back(obj());
		objs.back().assimp_load_from_mesh(mesh);
	}

	calculate_bounding_box();
}

void obj::load_from_string( stringstream& ss )
{
	clear();
	
	obj_raw obj;
	obj.load_from_string(ss);

	groups = obj.groups;
	objects = obj.objects;

	vector<cr::point3d>::iterator iv = obj.v.begin();
	for(; iv!= obj.v.end(); iv++)
		push_back_unique(v, *iv);

	vector<cr::point3d>::iterator ivn = obj.vn.begin();
	for(; ivn!= obj.vn.end(); ivn++)
		push_back_unique(vn, *ivn);

	vector<cr::point>::iterator ivt = obj.vt.begin();
	for(; ivt!= obj.vt.end(); ivt++)
		push_back_unique(vt, *ivt);

	vector<group_raw>::iterator g = obj.objs.begin();
	for(; g!= obj.objs.end(); g++)
	{
		obj_object object;
		object.group = g->g;
		object.object = g->o;

		vector<face_raw>::iterator f = g->f.begin();
		for(; f != g->f.end(); f++)
		{
			obj_face face;
			vector<face_point_raw>::iterator p = f->p.begin();
			int i = 0;
			for(; p != f->p.end(); p++, i++)
			{
				if(i >= 3)
				{
					number_of_indexes += 2;
					int last = face.indexes.back();
					face.indexes.push_back(face.indexes.front());
					face.indexes.push_back(last);
				}

				obj_face_point pf;

				if( p->v != -1)
					pf.v = cross_reference_vectors(v, obj.v, p->v);
				if( p->vt != -1)
					pf.vt = cross_reference_vectors(vt, obj.vt, p->vt);
				if( p->vn != -1)
					pf.vn = cross_reference_vectors(vn, obj.vn, p->vn);

				push_back_unique(points,pf);
				int index = find_index(points.begin(), points.end(), pf);

				face.indexes.push_back(index);	
				number_of_indexes++;
				
			}
			object.faces.push_back(face);
		}
		objs.push_back(object);
	}

	v_original = v;
	vt_original = vt;
	vn_original = vn;

	calculate_bounding_box();
}

void obj::clear()
{
	v.clear();
	vt.clear();
	vn.clear();
	v_original.clear();
	vt_original.clear();
	vn_original.clear();
	points.clear();
	objs.clear();
	number_of_indexes = 0;
	for(int i = 0; i < 8; i++) 
		bb[i] = cr::point3d(0,0,0);
	groups.clear();
	objects.clear();
}

void obj_raw::load_from_string( stringstream& ss )
{
	while( ss )
	{
		string currentLine;
		getline(ss, currentLine );

		if( currentLine.length() )
		{
			string temp;
			std::stringstream ss;
			ss << currentLine;
			ss >> temp;

			if( temp == "v" )
			{
				cr::point3d temp_v;
	
				ss >> temp_v.x;
				ss >> temp_v.y;
				ss >> temp_v.z;
				v.push_back(temp_v);
			}
			else if( temp == "vn" ) 
			{
				cr::point3d temp_vn;			

				ss >> temp_vn.x;
				ss >> temp_vn.y;
				ss >> temp_vn.z;

				vn.push_back( temp_vn );
			}
			else if( temp == "vt" ) 
			{
				cr::point temp_vt;

				//The line defines a vertex texture coord
				ss >> temp_vt.x;
				ss >> temp_vt.y;
				temp_vt.y = 1 - temp_vt.y;

				vt.push_back( temp_vt );
			}
			else if( temp == "f" )
			{
				face_raw temp_face;

				//The line defines a face
				while(ss)
				{
					string temp;
					ss >> temp;
					if( temp.length() )
					{
						face_point_raw temp_face_point;
						stringstream ss;

						ss << temp;

						// v
						if(ss){
							if( ss.peek() == '//')
								ss.ignore();
							else{
								ss >> temp_face_point.v;
								if(temp_face_point.v < 0)
								{
									temp_face_point.v += v.size();
								}
								else
									temp_face_point.v --;
							}
							ss.ignore();
						}

						// vt
						if(ss){
							if( ss.peek() == '//')
								ss.ignore();
							else{
								ss >> temp_face_point.vt;
								if(temp_face_point.vt < 0)
								{
									temp_face_point.vt += vt.size();
								}
								else
									temp_face_point.vt --;
							}
							ss.ignore();
						}

						// vn
						if(ss){
							if( ss.peek() == '//')
								ss.ignore();
							else{
								ss >> temp_face_point.vn;
								if(temp_face_point.vn < 0)
								{
									temp_face_point.vn += vn.size();
								}
								else
									temp_face_point.vn --;
							}
							ss.ignore();
						}

						temp_face.p.push_back(temp_face_point);
					}
				}

				objs.back().f.push_back(temp_face);

			} 
			else if( temp == "g" )
			{
				string name;
				ss >> name;

				group_raw prev = objs.back();
				if(!objs.back().f.size()) // remove empty containers
					objs.pop_back(); 

				objs.push_back( group_raw( prev.o, groups.size()));
				groups.push_back(name);
			}
			else if( temp == "o" )
			{
				string name;
				ss >> name;

				group_raw prev = objs.back();
				if(!objs.back().f.size()) // remove empty containers
					objs.pop_back(); 

				objs.push_back( group_raw( objects.size(), prev.g ));
				objects.push_back(name);
			}
			ss.ignore(1000, '\n' );
		}
	}
}

obj_raw::obj_raw()
{
	groups.push_back("default");
	objects.push_back("default");
	objs.push_back( group_raw(0,0) );
}
group_raw::group_raw( int _o, int _g )
{
	o = _o;
	g = _g;
}