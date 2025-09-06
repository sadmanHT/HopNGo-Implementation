'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useAuthStore } from '@/lib/state';
import { 
  Plus, 
  Edit, 
  Trash2,
  Phone,
  Mail,
  AlertTriangle,
  Shield,
  User
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

// Emergency contact types
interface EmergencyContact {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  relationship: string;
  isPrimary: boolean;
  createdAt: string;
  updatedAt: string;
}

interface EmergencyContactRequest {
  name: string;
  phoneNumber: string;
  email?: string;
  relationship: string;
  isPrimary: boolean;
}

export default function EmergencyPage() {
  const router = useRouter();
  const { user, token } = useAuthStore();
  
  const [contacts, setContacts] = useState<EmergencyContact[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingContact, setEditingContact] = useState<EmergencyContact | null>(null);
  const [triggerNote, setTriggerNote] = useState('');
  const [triggering, setTriggering] = useState(false);
  
  // Form state
  const [contactForm, setContactForm] = useState<EmergencyContactRequest>({
    name: '',
    phoneNumber: '',
    email: '',
    relationship: '',
    isPrimary: false
  });

  useEffect(() => {
    if (!user || !token) {
      router.push('/login');
      return;
    }
    fetchContacts();
  }, [user, token, router]);

  const fetchContacts = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/v1/emergency/contacts', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || '',
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data: EmergencyContact[] = await response.json();
        setContacts(data);
      }
    } catch (error) {
      console.error('Failed to fetch emergency contacts:', error);
    } finally {
      setLoading(false);
    }
  };

  const saveContact = async () => {
    try {
      const url = editingContact 
        ? `/api/v1/emergency/contacts/${editingContact.id}`
        : '/api/v1/emergency/contacts';
      
      const method = editingContact ? 'PATCH' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || '',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(contactForm)
      });
      
      if (response.ok) {
        const savedContact: EmergencyContact = await response.json();
        
        if (editingContact) {
          setContacts(prev => prev.map(c => c.id === savedContact.id ? savedContact : c));
        } else {
          setContacts(prev => [savedContact, ...prev]);
        }
        
        resetForm();
      }
    } catch (error) {
      console.error('Failed to save emergency contact:', error);
    }
  };

  const deleteContact = async (id: string) => {
    try {
      const response = await fetch(`/api/v1/emergency/contacts/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || ''
        }
      });
      
      if (response.ok) {
        setContacts(prev => prev.filter(c => c.id !== id));
      }
    } catch (error) {
      console.error('Failed to delete emergency contact:', error);
    }
  };

  const triggerEmergency = async () => {
    try {
      setTriggering(true);
      const response = await fetch('/api/v1/emergency/trigger', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || '',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ note: triggerNote })
      });
      
      if (response.ok) {
        alert('Emergency alert sent to your contacts!');
        setTriggerNote('');
      }
    } catch (error) {
      console.error('Failed to trigger emergency:', error);
      alert('Failed to send emergency alert. Please try again.');
    } finally {
      setTriggering(false);
    }
  };

  const resetForm = () => {
    setContactForm({
      name: '',
      phoneNumber: '',
      email: '',
      relationship: '',
      isPrimary: false
    });
    setShowForm(false);
    setEditingContact(null);
  };

  const startEdit = (contact: EmergencyContact) => {
    setContactForm({
      name: contact.name,
      phoneNumber: contact.phoneNumber,
      email: contact.email || '',
      relationship: contact.relationship,
      isPrimary: contact.isPrimary
    });
    setEditingContact(contact);
    setShowForm(true);
  };

  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold mb-4">Access Denied</h2>
          <p className="text-gray-600 mb-4">Please log in to access emergency contacts.</p>
          <Button onClick={() => router.push('/login')}>Go to Login</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2 flex items-center gap-2">
          <Shield className="h-8 w-8 text-red-600" />
          Emergency Contacts
        </h1>
        <p className="text-gray-600">
          Manage your emergency contacts and trigger alerts when needed.
        </p>
      </div>

      {/* Emergency Trigger Section */}
      <Card className="mb-8 border-red-200 bg-red-50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-red-700">
            <AlertTriangle className="h-5 w-5" />
            Emergency Alert
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <Label htmlFor="note">Additional Note (Optional)</Label>
              <Textarea
                id="note"
                placeholder="Describe your situation or location..."
                value={triggerNote}
                onChange={(e) => setTriggerNote(e.target.value)}
                className="mt-1"
              />
            </div>
            <Button 
              onClick={triggerEmergency}
              disabled={triggering || contacts.length === 0}
              className="w-full bg-red-600 hover:bg-red-700"
            >
              {triggering ? 'Sending Alert...' : 'Send Emergency Alert'}
            </Button>
            {contacts.length === 0 && (
              <p className="text-sm text-red-600">
                Add emergency contacts below to enable emergency alerts.
              </p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Contacts Management */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-semibold">Your Emergency Contacts</h2>
        <Button 
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2"
        >
          <Plus className="h-4 w-4" />
          Add Contact
        </Button>
      </div>

      {/* Contact Form */}
      {showForm && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>
              {editingContact ? 'Edit Contact' : 'Add New Contact'}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="name">Name *</Label>
                <Input
                  id="name"
                  value={contactForm.name}
                  onChange={(e) => setContactForm(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="Contact name"
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="phone">Phone Number *</Label>
                <Input
                  id="phone"
                  value={contactForm.phoneNumber}
                  onChange={(e) => setContactForm(prev => ({ ...prev, phoneNumber: e.target.value }))}
                  placeholder="+1234567890"
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={contactForm.email}
                  onChange={(e) => setContactForm(prev => ({ ...prev, email: e.target.value }))}
                  placeholder="contact@example.com"
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="relationship">Relationship *</Label>
                <Input
                  id="relationship"
                  value={contactForm.relationship}
                  onChange={(e) => setContactForm(prev => ({ ...prev, relationship: e.target.value }))}
                  placeholder="e.g., Spouse, Parent, Friend"
                  className="mt-1"
                />
              </div>
            </div>
            <div className="mt-4 flex items-center space-x-2">
              <input
                type="checkbox"
                id="isPrimary"
                checked={contactForm.isPrimary}
                onChange={(e) => setContactForm(prev => ({ ...prev, isPrimary: e.target.checked }))}
                className="rounded"
              />
              <Label htmlFor="isPrimary">Primary contact</Label>
            </div>
            <div className="flex gap-2 mt-6">
              <Button 
                onClick={saveContact}
                disabled={!contactForm.name || !contactForm.phoneNumber || !contactForm.relationship}
              >
                {editingContact ? 'Update Contact' : 'Add Contact'}
              </Button>
              <Button variant="outline" onClick={resetForm}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Contacts List */}
      {loading ? (
        <div className="text-center py-8">
          <p>Loading contacts...</p>
        </div>
      ) : contacts.length === 0 ? (
        <Card>
          <CardContent className="text-center py-8">
            <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold mb-2">No Emergency Contacts</h3>
            <p className="text-gray-600 mb-4">
              Add emergency contacts to enable emergency alerts.
            </p>
            <Button onClick={() => setShowForm(true)}>
              Add Your First Contact
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {contacts.map((contact) => (
            <Card key={contact.id} className={contact.isPrimary ? 'border-blue-200 bg-blue-50' : ''}>
              <CardContent className="p-4">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="font-semibold">{contact.name}</h3>
                      {contact.isPrimary && (
                        <span className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
                          Primary
                        </span>
                      )}
                    </div>
                    <div className="space-y-1 text-sm text-gray-600">
                      <div className="flex items-center gap-2">
                        <Phone className="h-4 w-4" />
                        {contact.phoneNumber}
                      </div>
                      {contact.email && (
                        <div className="flex items-center gap-2">
                          <Mail className="h-4 w-4" />
                          {contact.email}
                        </div>
                      )}
                      <p><strong>Relationship:</strong> {contact.relationship}</p>
                      <p className="text-xs">
                        Added {formatDistanceToNow(new Date(contact.createdAt))} ago
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => startEdit(contact)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => deleteContact(contact.id)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}