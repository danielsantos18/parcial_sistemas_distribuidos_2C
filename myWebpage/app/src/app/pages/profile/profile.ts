import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss']
})
export class ProfileComponent implements OnInit {
  user: any = null;
  loading = true;


  constructor(private http: HttpClient) {}


  ngOnInit() {
    const userId = 'e25851fb-b263-48e9-bb97-90b27c2a8bd8';
    const url = `https://v7hjhcn0ej.execute-api.us-east-2.amazonaws.com/users/profile/${userId}`;


    this.http.get(url).subscribe({
      next: (res: any) => {
        this.user = res;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading profile', err);
        this.loading = false;
      }
    });
  }
}